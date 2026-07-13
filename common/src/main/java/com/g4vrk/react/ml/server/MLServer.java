package com.g4vrk.react.ml.server;

import com.g4vrk.react.ml.http.model.HttpRequest;
import com.g4vrk.react.ml.server.settings.MLClientSettings;
import lombok.AccessLevel;
import lombok.Getter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

@Getter
public class MLServer {

    @Getter(AccessLevel.NONE)
    private final OkHttpClient client;
    private final String serverUrl;
    private final String apiKey;

    public MLServer(
            @NotNull MLClientSettings settings
    ) {
        this.client = settings.buildClient();
        this.serverUrl = settings.getServerUrl();
        this.apiKey = settings.getApiKey();
    }

    public @NotNull Call newCall(
            final @NotNull Request request
    ) {
        return this.client.newCall(request);
    }

    public @NotNull Call newCall(
            final @NotNull HttpRequest httpRequest
    ) {
        final Request request = new Request.Builder()
                .url(serverUrl + httpRequest.getPath())
                .method(httpRequest.getMethod(), httpRequest.getBody())
                .addHeader("X-Subscription-Token", apiKey)
                .build();

        return this.newCall(request);
    }

    public void callAsync(
            final @NotNull HttpRequest request,
            final @NotNull Callback callback
    ) {
        this.newCall(request).enqueue(callback);
    }
}
