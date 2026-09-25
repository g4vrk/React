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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class InferenceAimProcessor {

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

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
                    final String responseText =
                            body == null ? "" : body.string();

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
