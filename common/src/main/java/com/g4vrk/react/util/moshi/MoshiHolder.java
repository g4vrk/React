package com.g4vrk.react.util.moshi;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.Types;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public final class MoshiHolder {

    public static final Moshi INSTANCE = new Moshi.Builder()
            .add(new UUIDAdapter())
            .build();

    public static final Type REQUEST_TYPE = Types.newParameterizedType(Map.class, String.class, Object.class);
    public static final Type RESPONSE_TYPE = Types.newParameterizedType(Map.class, String.class, Double.class);

    public static final JsonAdapter<Map<String, Object>> REQUEST_ADAPTER = INSTANCE.adapter(REQUEST_TYPE);
    public static final JsonAdapter<Map<String, Double>> RESPONSE_ADAPTER = INSTANCE.adapter(RESPONSE_TYPE);

    public static class UUIDAdapter {
        @ToJson
        public String toJson(UUID uuid) {
            return uuid.toString();
        }

        @FromJson
        public UUID fromJson(String json) {
            return UUID.fromString(json);
        }
    }

}