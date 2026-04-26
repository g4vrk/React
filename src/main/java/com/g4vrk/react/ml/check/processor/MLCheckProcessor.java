package com.g4vrk.react.ml.check.processor;

import com.g4vrk.react.game.Rotation;
import com.g4vrk.react.player.LocalPlayer;
import com.g4vrk.react.ml.http.model.HttpRequest;
import com.g4vrk.react.ml.server.MLServer;
import com.g4vrk.react.moshi.MoshiHolder;
import com.g4vrk.react.runner.TaskRunner;
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
import java.util.Map;

public final class MLCheckProcessor {

    private final Logger logger;

    private final MLServer mlServer;
    private final TaskRunner taskRunner;

    private final JsonAdapter<Map<String, Object>> requestAdapter;
    private final JsonAdapter<Map<String, Double>> responseAdapter;

    public MLCheckProcessor(
            @NotNull Logger logger,
            @NotNull MLServer mlServer,
            @NotNull TaskRunner taskRunner
    ) {
        this.logger = logger;
        this.mlServer = mlServer;
        this.taskRunner = taskRunner;
        this.requestAdapter = MoshiHolder.REQUEST_ADAPTER;
        this.responseAdapter = MoshiHolder.RESPONSE_ADAPTER;
    }

    public void check(
            final @NotNull LocalPlayer entity
    ) {
        final Rotation[] rotations = entity.snapshotRotations();
        if (rotations.length == 0) return;

        final Map<String, Object> payload = Map.of(
                "name", entity.getName(),
                "frames", rotations
        );

        final String json = requestAdapter.toJson(payload);

        final HttpRequest request = new HttpRequest(
                "/analyze",
                "POST",
                RequestBody.create(
                        json,
                        MediaType.get("application/json")
                )
        );

        mlServer.callAsync(request, new Callback() {

            @Override
            public void onFailure(
                    @NotNull Call call,
                    @NotNull IOException ex
            ) {
                logger.info("При отправке запроса на удаленный сервер произошла ошибка.", ex);
            }

            @Override
            public void onResponse(
                    @NotNull Call call,
                    @NotNull Response response
            ) throws IOException {
                try (final ResponseBody body = response.body()) {

                    if (!response.isSuccessful() || body == null) return;

                    final Map<String, Double> result = responseAdapter.fromJson(body.string());

                    if (result == null) return;

                    final Double probability = result.get("cheat_probability");
                    if (probability == null) return;

                    taskRunner.runTask(() -> handle(entity, probability));
                }
            }
        });
    }

    private void handle(
            final @NotNull LocalPlayer entity,
            final double probability
    ) {
//        final double threshold = SolarAI.getInstance()
//                .getConfig()
//                .getDouble("ml-check.classification-threshold");

//        plugin.getViolationDatabase()
//                .saveViolation(entity.getUuid(), entity.getName(), probability);
//
//        if (probability <= threshold) return;
//
//        plugin.getViolationManager()
//                .handleViolation(entity, probability);

        entity.clearRotation();
    }
}