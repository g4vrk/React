package com.g4vrk.react.inference.aim;

import com.g4vrk.react.inference.http.model.HttpRequest;
import com.g4vrk.react.inference.server.InferenceServer;
import com.g4vrk.react.inference.server.settings.InferenceRequestSettings;
import com.g4vrk.react.inference.server.settings.InferenceResponseSettings;
import com.g4vrk.react.player.model.rotation.Rotation;
import com.g4vrk.react.util.moshi.MoshiHolder;
import com.g4vrk.schedula.task.TickSchedule;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import com.squareup.moshi.JsonAdapter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import com.g4vrk.react.universe.UniverseFrame;
import com.g4vrk.react.universe.UniverseProtocol;
import java.nio.charset.StandardCharsets;

public final class InferenceAimProcessor {

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    private static final long MAX_RESPONSE_BYTES = 64L * 1024L;

    private final Logger logger;
    private final InferenceServer inferenceServer;
    private final Scheduler scheduler;

    private final JsonAdapter<Map<String, Object>> requestAdapter;
    private final JsonAdapter<Map<String, Object>> responseAdapter;

    public InferenceAimProcessor(
            @NotNull Logger logger,
            @NotNull InferenceServer inferenceServer,
            @NotNull Scheduler scheduler
    ) {
        this.logger = logger;
        this.inferenceServer = inferenceServer;
        this.scheduler = scheduler;

        this.requestAdapter = MoshiHolder.REQUEST_ADAPTER;
        this.responseAdapter = MoshiHolder.RESPONSE_ADAPTER;
    }

    public void check(
            final @NotNull String playerName,
            final @NotNull Rotation @NotNull [] snapshot,
            final @NotNull Consumer<InferenceResult> resultHandler
    ) {
        checkFrames(playerName, snapshot, snapshot.length, resultHandler);
    }

    public void check(
            final @NotNull String playerName,
            final @NotNull UniverseFrame[] snapshot,
            final @NotNull Consumer<InferenceResult> resultHandler
    ) {
        if (!inferenceServer.isEnabled() || snapshot.length == 0) {
            complete(resultHandler, InferenceResult.unavailable());
            return;
        }
        try {
            final byte[] batch = UniverseProtocol.encodeBatch(snapshot);
            final HttpRequest request = new HttpRequest("POST",
                    RequestBody.create(batch, MediaType.get("application/x-flatbuffers")),
                    Map.of("Accept", "application/x-flatbuffers", "X-Batch", "1"));
            enqueue(playerName, request, resultHandler, true);
        } catch (RuntimeException failure) {
            logger.warn("Could not enqueue UNIVERSE request for {}", playerName, failure);
            complete(resultHandler, InferenceResult.unavailable());
        }
    }

    private void checkFrames(String playerName, Object snapshot, int frameCount,
                             Consumer<InferenceResult> resultHandler) {
        if (!inferenceServer.isEnabled()) {
            complete(resultHandler, InferenceResult.unavailable());
            return;
        }

        if (frameCount < 3) {
            logger.debug(
                    "Not enough rotation frames for {}: {}",
                    playerName,
                    frameCount
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
            json = requestAdapter.toJson(payload);
        } catch (final Exception ex) {
            logger.warn("Could not serialize ML request for {}", playerName, ex);
            complete(resultHandler, InferenceResult.unavailable());
            return;
        }

        final HttpRequest request = new HttpRequest(
                "POST",
                RequestBody.create(json, JSON)
        );

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
        enqueue(playerName, request, resultHandler, false);
    }

    private void enqueue(String playerName, HttpRequest request,
                         Consumer<InferenceResult> resultHandler, boolean universe) {
        inferenceServer.callAsync(request, new Callback() {

            @Override
            public void onFailure(
                    @NotNull Call call,
                    @NotNull IOException ex
            ) {
                logger.warn(
                        "Could not send rotation frames for {}",
                        playerName,
                        ex
                );

                complete(resultHandler, InferenceResult.unavailable());
            }

            @Override
            public void onResponse(
                    @NotNull Call call,
                    @NotNull Response response
            ) {
                try (ResponseBody body = response.body()) {
                    final String responseText;
                    if (body == null) {
                        responseText = "";
                    } else {
                        final BufferedSource source = body.source();
                        if (source.request(MAX_RESPONSE_BYTES + 1L)) {
                            logger.warn(
                                    "ML server response is too large for {}: more than {} bytes",
                                    playerName,
                                    MAX_RESPONSE_BYTES
                            );
                            complete(resultHandler, InferenceResult.unavailable());
                            return;
                        }
                        if (universe && response.isSuccessful()) {
                            final byte[] bytes = source.readByteArray();
                            if (bytes.length >= 4 && bytes[0] == 'G' && bytes[1] == 'A'
                                    && bytes[2] == 'I' && bytes[3] == 'B') {
                                complete(resultHandler, new InferenceResult(
                                        UniverseProtocol.decodeResponse(bytes)));
                                return;
                            }
                            responseText = new String(bytes, StandardCharsets.UTF_8);
                        } else {
                            responseText = source.readUtf8();
                        }
                    }

                    if (!response.isSuccessful()) {
                        logger.warn(
                                "ML server rejected request for {}: HTTP {} {}",
                                playerName,
                                response.code(),
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

                    final Map<String, Object> result =
                            responseAdapter.fromJson(responseText);

                    if (result == null) {
                        logger.warn(
                                "Could not parse ML response for {}: {}",
                                playerName,
                                responseText
                        );

                        complete(resultHandler, InferenceResult.unavailable());
                        return;
                    }

                    final InferenceResponseSettings responseSettings =
                            inferenceServer.getResponseSettings();

                    final Double probability = readNumber(
                            result,
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

                    complete(
                            resultHandler,
                            new InferenceResult(probability)
                    );
                } catch (Exception ex) {
                    logger.warn(
                            "Could not process ML response for {}",
                            playerName,
                            ex
                    );

                    complete(resultHandler, InferenceResult.unavailable());
                }
            }
        });
    }

    private static Double readNumber(
            final @NotNull Map<String, Object> response,
            final @NotNull String field
    ) {
        final Object raw = response.get(field);
        return raw instanceof Number number ? number.doubleValue() : null;
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
}
