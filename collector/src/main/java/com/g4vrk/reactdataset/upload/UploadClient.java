package com.g4vrk.reactdataset.upload;

import com.g4vrk.reactdataset.model.DatasetFrame;
import com.g4vrk.reactdataset.upload.model.UploadResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

public final class UploadClient {
    private final HttpClient http;
    private final URI uploadUri;
    private final String bearerToken;
    private final Duration timeout;

    public UploadClient(String baseUrl, String bearerToken, long timeoutSeconds) {
        this.uploadUri = URI.create(UploadClient.trimTrailingSlash(baseUrl) + "/upload");
        this.bearerToken = bearerToken == null ? "" : bearerToken.trim();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    public URI uploadUri() {
        return this.uploadUri;
    }

    public UploadResult upload(String playerName, boolean cheater, List<DatasetFrame> frames) {
        HttpResponse<String> response;
        HttpRequest.Builder builder = HttpRequest.newBuilder(this.uploadUri).timeout(this.timeout).header("Content-Type", "application/json; charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(UploadClient.buildBody(playerName, cheater, frames)));
        if (!this.bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + this.bearerToken);
        }
        try {
            response = this.http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
        catch (IOException ex) {
            return UploadResult.RETRY;
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return UploadResult.RETRY;
        }
        int code = response.statusCode();
        if (code == 401 || code == 403) {
            return UploadResult.FORBIDDEN;
        }
        if (code == 429 || code >= 500) {
            return UploadResult.RETRY;
        }
        if (code >= 400) {
            return UploadResult.REJECTED;
        }
        return UploadClient.isDuplicate(response.body()) ? UploadResult.DUPLICATE : UploadResult.OK;
    }

    private static boolean isDuplicate(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String compact = body.toLowerCase(Locale.ROOT).replace(" ", "").replace("\n", "").replace("\r", "");
        return compact.contains("\"status\":\"duplicate\"");
    }

    private static String buildBody(String playerName, boolean cheater, List<DatasetFrame> frames) {
        if (!frames.isEmpty() && frames.get(0).universe() != null) {
            return buildUniverseBody(playerName, cheater, frames);
        }
        StringBuilder json = new StringBuilder(frames.size() * 128 + 96);
        json.append("{\"player_name\":").append(UploadClient.quote(playerName)).append(",\"is_cheater\":").append(cheater).append(",\"frames\":[");
        for (int i = 0; i < frames.size(); ++i) {
            if (i != 0) {
                json.append(',');
            }
            DatasetFrame f = frames.get(i);
            json.append("{\"x\":").append(UploadClient.num(f.x())).append(",\"y\":").append(UploadClient.num(f.y())).append(",\"deltaX\":").append(UploadClient.num(f.deltaX())).append(",\"deltaY\":").append(UploadClient.num(f.deltaY())).append(",\"jerkX\":").append(UploadClient.num(f.jerkX())).append(",\"jerkY\":").append(UploadClient.num(f.jerkY())).append(",\"gcdErrorX\":").append(UploadClient.num(f.gcdErrorX())).append(",\"gcdErrorY\":").append(UploadClient.num(f.gcdErrorY())).append('}');
        }
        return json.append("]}").toString();
    }

    private static String buildUniverseBody(String playerName, boolean cheater, List<DatasetFrame> frames) {
        StringBuilder json = new StringBuilder();
        json.append("{\"player_name\":").append(quote(playerName))
                .append(",\"is_cheater\":").append(cheater)
                .append(",\"model\":\"UNIVERSE\",\"frames\":[");
        for (int row = 0; row < frames.size(); row++) {
            if (row > 0) json.append(',');
            json.append('[');
            float[] values = frames.get(row).universe().datasetFeatures();
            for (int column = 0; column < values.length; column++) {
                if (column > 0) json.append(',');
                json.append(Float.toString(values[column]));
            }
            json.append(']');
        }
        return json.append("]}").toString();
    }

    private static String num(float value) {
        return Float.isFinite(value) ? Float.toString(value) : "0";
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8).append('\"');
        block7: for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': {
                    out.append("\\\\");
                    continue block7;
                }
                case '\"': {
                    out.append("\\\"");
                    continue block7;
                }
                case '\n': {
                    out.append("\\n");
                    continue block7;
                }
                case '\r': {
                    out.append("\\r");
                    continue block7;
                }
                case '\t': {
                    out.append("\\t");
                    continue block7;
                }
                default: {
                    out.append(c);
                }
            }
        }
        return out.append('\"').toString();
    }

    private static String trimTrailingSlash(String value) {
        String out;
        String string = out = value == null || value.isBlank() ? "http://127.0.0.1:8000" : value.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}
