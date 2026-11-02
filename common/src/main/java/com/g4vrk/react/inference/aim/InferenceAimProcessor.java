package com.g4vrk.react.inference.aim;

import com.g4vrk.react.inference.http.model.HttpRequest;
import com.g4vrk.react.inference.json.InferenceJsonCodec;
import com.g4vrk.react.inference.server.InferenceServer;
import com.g4vrk.react.inference.server.settings.InferenceRequestSettings;
import com.g4vrk.react.inference.server.settings.InferenceResponseSettings;
import com.g4vrk.react.player.model.rotation.Rotation;
import com.g4vrk.schedula.task.TickSchedule;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

public final class InferenceAimProcessor {

    private static final int MAX_RESPONSE_BYTES = 64 * 1024;

    private final Logger logger;
    private final InferenceServer inferenceServer;
    private final Scheduler scheduler;

    public InferenceAimProcessor(
            @NotNull Logger logger,
            @NotNull InferenceServer inferenceServer,
            @NotNull Scheduler scheduler
    ) {
        this.logger = logger;
        this.inferenceServer = inferenceServer;
        this.scheduler = scheduler;
    }

    public void check(
            final @NotNull String playerName,
            final @NotNull Rotation @NotNull [] snapshot,
            final @NotNull Consumer<InferenceResult> resultHandler
    ) {
        if (!inferenceServer.isEnabled()) {
            complete(resultHandler, InferenceResult.unavailable());
            return;
        }

        if (snapshot.length < 3) {
            logger.debug(
                    "Not enough rotation frames for {}: {}",
                    playerName,
                    snapshot.length
            );
            complete(resultHandler, InferenceResult.unavailable());
            return;
        }

        final InferenceRequestSettings requestSettings = inferenceServer.getRequestSettings();

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(requestSettings.getPlayerNameField(), playerName);
        payload.put(requestSettings.getRotationsField(), snapshot);

        inferenceServer.augmentPayload(payload);

        final String json;

        try {
            json = InferenceJsonCodec.writeObject(payload);
        } catch (final Exception ex) {
            logger.warn("Could not serialize ML request for {}", playerName, ex);
            complete(resultHandler, InferenceResult.unavailable());
            return;
        }

        final HttpRequest request = new HttpRequest("POST", json);

        try {
            enqueue(playerName, request, resultHandler);
        } catch (final Throwable th) {
            logger.warn("Could not enqueue ML request for {}", playerName, th);
            complete(resultHandler, InferenceResult.unavailable());
        }
    }

    private void enqueue(
            final @NotNull String playerName,
            final @NotNull HttpRequest request,
            final @NotNull Consumer<InferenceResult> resultHandler
    ) {
        inferenceServer.callAsync(request, responseBodyHandler())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        if (isResponseTooLarge(throwable)) {
                            logger.warn(
                                    "ML server response is too large for {}: more than {} bytes",
                                    playerName,
                                    MAX_RESPONSE_BYTES
                            );
                        } else {
                            logger.warn(
                                    "Could not send rotation frames for {}",
                                    playerName,
                                    throwable
                            );
                        }

                        complete(resultHandler, InferenceResult.unavailable());
                        return;
                    }

                    try {
                        final String responseText = new String(response.body(), StandardCharsets.UTF_8);

                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            logger.warn(
                                    "ML server rejected request for {}: HTTP {} {}",
                                    playerName,
                                    response.statusCode(),
                                    responseText
                            );

                            complete(resultHandler, InferenceResult.unavailable());
                            return;
                        }

                        if (responseText.isBlank()) {
                            logger.warn(
                                    "ML server returned an empty response for {}",
                                    playerName
                            );

                            complete(resultHandler, InferenceResult.unavailable());
                            return;
                        }

                        final InferenceResponseSettings responseSettings =
                                inferenceServer.getResponseSettings();

                        final Double probability = InferenceJsonCodec.readNumberField(
                                responseText,
                                responseSettings.getProbabilityField()
                        );

                        if (probability == null || !Double.isFinite(probability)
                                || probability < 0.0D || probability > 1.0D) {
                            logger.warn(
                                    "ML response does not contain a valid '{}' field for {}: {}",
                                    responseSettings.getProbabilityField(),
                                    playerName,
                                    responseText
                            );

                            complete(resultHandler, InferenceResult.unavailable());
                            return;
                        }

                        complete(resultHandler, new InferenceResult(probability));
                    } catch (final Exception ex) {
                        logger.warn(
                                "Could not process ML response for {}",
                                playerName,
                                ex
                        );

                        complete(resultHandler, InferenceResult.unavailable());
                    }
                });
    }

    private static @NotNull HttpResponse.BodyHandler<byte[]> responseBodyHandler() {
        return responseInfo -> new LimitedBodySubscriber(MAX_RESPONSE_BYTES);
    }

    private static boolean isResponseTooLarge(final @NotNull Throwable throwable) {
        Throwable cause = throwable;

        while (cause != null) {
            if (cause instanceof ResponseTooLargeException) {
                return true;
            }

            if (cause.getCause() == cause) {
                break;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private void complete(
            @NotNull Consumer<InferenceResult> resultHandler,
            @NotNull InferenceResult result
    ) {
        scheduler.schedule(
                () -> resultHandler.accept(result),
                TickSchedule.instant()
        );
    }

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {

        private final int maximumBytes;
        private final ByteArrayOutputStream output;
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();

        private Flow.Subscription subscription;

        private LimitedBodySubscriber(final int maximumBytes) {
            this.maximumBytes = maximumBytes;
            this.output = new ByteArrayOutputStream(Math.min(maximumBytes, 8192));
        }

        @Override
        public @NotNull CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(final @NotNull Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(final @NotNull List<ByteBuffer> buffers) {
            for (final ByteBuffer buffer : buffers) {
                final int remaining = buffer.remaining();

                if (remaining > maximumBytes - output.size()) {
                    subscription.cancel();
                    body.completeExceptionally(new ResponseTooLargeException(maximumBytes));
                    return;
                }

                final byte[] bytes = new byte[remaining];
                buffer.get(bytes);
                output.write(bytes, 0, bytes.length);
            }

            subscription.request(1);
        }

        @Override
        public void onError(final @NotNull Throwable throwable) {
            body.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            body.complete(output.toByteArray());
        }
    }

    private static final class ResponseTooLargeException extends IOException {

        private static final long serialVersionUID = 1L;

        private ResponseTooLargeException(final int maximumBytes) {
            super("HTTP response exceeds " + maximumBytes + " bytes");
        }
    }
}
