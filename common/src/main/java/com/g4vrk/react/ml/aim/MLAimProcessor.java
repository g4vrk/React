package com.g4vrk.react.ml.aim;

import com.g4vrk.react.api.task.runner.TaskRunner;
import com.g4vrk.react.api.task.schedule.TickSchedule;
import com.g4vrk.react.ml.http.model.HttpRequest;
import com.g4vrk.react.ml.server.MLServer;
import com.g4vrk.react.player.model.rotation.Rotation;
import com.g4vrk.react.util.moshi.MoshiHolder;
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

public final class MLAimProcessor {

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    private final Logger logger;
    private final MLServer mlServer;
    private final TaskRunner taskRunner;
    private final String subscriptionToken;

    private final JsonAdapter<Map<String, Object>> requestAdapter;
    private final JsonAdapter<Map<String, Double>> responseAdapter;

    public MLAimProcessor(
            @NotNull Logger logger,
            @NotNull MLServer mlServer,
            @NotNull TaskRunner taskRunner
    ) {
        this.logger = logger;
        this.mlServer = mlServer;
        this.taskRunner = taskRunner;
        this.subscriptionToken = mlServer.getApiKey().trim();

        this.requestAdapter = MoshiHolder.REQUEST_ADAPTER;
        this.responseAdapter = MoshiHolder.RESPONSE_ADAPTER;
    }

    public void check(
            final @NotNull String playerName,
            final @NotNull Rotation @NotNull [] snapshot,
            final @NotNull Consumer<Double> resultHandler
    ) {
        if (!mlServer.isEnabled()) {
            complete(resultHandler, -1.0D);
            return;
        }

        if (snapshot.length < 3) {
            logger.debug(
                    "Not enough rotation frames for {}: {}",
                    playerName,
                    snapshot.length
            );
            complete(resultHandler, -1.0D);
            return;
        }

        if (subscriptionToken.isEmpty()) {
            logger.warn("ML subscription token is empty");
            complete(resultHandler, -1.0D);
            return;
        }

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", playerName);
        payload.put("frames", snapshot);
        payload.put("token", subscriptionToken);

        final String json;

        try {
            json = requestAdapter.toJson(payload);
        } catch (Exception ex) {
            logger.warn("Could not serialize ML request for {}", playerName, ex);
            complete(resultHandler, -1.0D);
            return;
        }

        final HttpRequest request = new HttpRequest(
                "/analyze",
                "POST",
                RequestBody.create(json, JSON)
        );

        mlServer.callAsync(request, new Callback() {

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

                complete(resultHandler, -1.0D);
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

                        complete(resultHandler, -1.0D);
                        return;
                    }

                    if (responseText.isBlank()) {
                        logger.warn(
                                "ML server returned an empty response for {}",
                                playerName
                        );

                        complete(resultHandler, -1.0D);
                        return;
                    }

                    final Map<String, Double> result =
                            responseAdapter.fromJson(responseText);

                    if (result == null) {
                        logger.warn(
                                "Could not parse ML response for {}: {}",
                                playerName,
                                responseText
                        );

                        complete(resultHandler, -1.0D);
                        return;
                    }

                    final Double probability =
                            result.get("cheat_probability");

                    if (probability == null || !Double.isFinite(probability)) {
                        logger.warn(
                                "ML response does not contain a valid "
                                        + "cheat_probability for {}: {}",
                                playerName,
                                responseText
                        );

                        complete(resultHandler, -1.0D);
                        return;
                    }

                    complete(
                            resultHandler,
                            Math.max(0.0D, Math.min(1.0D, probability))
                    );
                } catch (Exception ex) {
                    logger.warn(
                            "Could not process ML response for {}",
                            playerName,
                            ex
                    );

                    complete(resultHandler, -1.0D);
                }
            }
        });
    }

    private void complete(
            @NotNull Consumer<Double> resultHandler,
            double result
    ) {
        taskRunner.runTask(
                () -> resultHandler.accept(result),
                TickSchedule.instant()
        );
    }
}