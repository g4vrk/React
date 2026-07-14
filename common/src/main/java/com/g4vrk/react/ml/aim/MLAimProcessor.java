package com.g4vrk.react.ml.aim;

import com.g4vrk.react.alert.printer.AlertPrinter;
import com.g4vrk.react.api.task.schedule.TickSchedule;
import com.g4vrk.react.color.resolver.ValueColorResolver;
import com.g4vrk.react.color.resolver.impl.ProbabilityColorResolver;
import com.g4vrk.react.game.Rotation;
import com.g4vrk.react.player.model.ReactPlayer;
import com.g4vrk.react.ml.http.model.HttpRequest;
import com.g4vrk.react.ml.server.MLServer;
import com.g4vrk.react.moshi.MoshiHolder;
import com.g4vrk.react.api.task.runner.TaskRunner;
import com.squareup.moshi.JsonAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
import java.util.function.Consumer;

public final class MLAimProcessor {

    private final Logger logger;

    private final MLServer mlServer;
    private final TaskRunner taskRunner;

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
        this.requestAdapter = MoshiHolder.REQUEST_ADAPTER;
        this.responseAdapter = MoshiHolder.RESPONSE_ADAPTER;
    }

    public void check(
            final @NotNull String playerName,
            final @NotNull Rotation @NotNull [] snapshot,
            final @NotNull Consumer<Double> resultHandler
    ) {
        if (snapshot.length == 0) return;

        final Map<String, Object> payload = Map.of(
                "name", playerName,
                "frames", snapshot
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
                logger.info("An internal error occurred when trying to check player rotation frames", ex);
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

                    taskRunner.runTask(() -> resultHandler.accept(probability), TickSchedule.instant());
                }
            }
        });
    }

//    private void handle(
//            final @NotNull ReactPlayer entity,
//            final double probability
//    ) {
//        final TextColor color = colorResolver.resolve(probability);
//        final Component verbose = Component.text(probability * 100.0D + "%")
//                .color(color);
//
//        alertPrinter.print(entity, "MLCheck-Aim", verbose);
//
//        entity.getRotationData().clear();
//    }
}