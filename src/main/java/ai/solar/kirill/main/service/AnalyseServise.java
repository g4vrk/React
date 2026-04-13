package ai.solar.kirill.main.service;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okhttp3.*;
import org.bukkit.Bukkit;
import ai.solar.kirill.SolarAI;
import ai.solar.kirill.utils.govno.MoshiFactory;
import ai.solar.kirill.utils.igrok.PlayerEntity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class AnalyseServise {

    public static void analyze(final PlayerEntity entity) {
        if (!SolarAI.isServerActive() || entity.getFrames().isEmpty()) {
            return;
        }
        entity.setLastAnalyzedFrames(entity.getFrames());

        // get key config
        String apiKey = SolarAI.getInstance().getConfig().getString("server.api-key", "nety key");

        String json;
        try {
            Map<String, Object> dataToSend = new HashMap<>();
            dataToSend.put("name", entity.getName());
            dataToSend.put("frames", entity.getLastAnalyzedFrames());
            dataToSend.put("token", apiKey); // key v zapros

            Moshi moshi = MoshiFactory.getInstance();
            Type mapType = Types.newParameterizedType(Map.class, String.class, Object.class);
            JsonAdapter<Map<String, Object>> adapter = moshi.adapter(mapType);
            json = adapter.toJson(dataToSend);
        } catch (Exception e) {
            return;
        }

        String url = SolarAI.getInstance().getServerUrl() + "/analyze";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("X-Subscription-Token", apiKey)
                .build();

        SolarAI.getInstance().getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        return;
                    }
                    String bodyString = responseBody.string();
                    Map<String, Double> result = LazyHolder.MAP_ADAPTER.fromJson(bodyString);
                    if (result != null && result.containsKey("cheat_probability")) {
                        double probability = result.get("cheat_probability");
                        Bukkit.getScheduler().runTask(SolarAI.getInstance(), () -> {
                            handleAnalysisResult(entity, probability);
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private static void handleAnalysisResult(PlayerEntity entity, double probability) {
        SolarAI plugin = SolarAI.getInstance();
        double classificationThreshold = plugin.getConfig().getDouble("ml-check.classification-threshold");

        plugin.getViolationDatabase().saveViolation(entity.getUuid(), entity.getName(), probability);

        if (probability <= classificationThreshold) {
            return;
        }

        plugin.getViolationManager().handleViolation(entity, probability);
    }

    private static class LazyHolder {
        private static final Moshi MOSHI = MoshiFactory.getInstance();
        private static final Type MAP_TYPE = Types.newParameterizedType(Map.class, String.class, Double.class);
        private static final JsonAdapter<Map<String, Double>> MAP_ADAPTER = MOSHI.adapter(MAP_TYPE);
    }
}