package ai.solar.kirill.utils.govno;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.Types;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public class MoshiFactory {
    private static final Moshi INSTANCE = new Moshi.Builder()
            .add(new UUIDAdapter())
            .build();

    private static final Type MAP_TYPE = Types.newParameterizedType(Map.class, String.class, Object.class);
    private static final JsonAdapter<Map<String, Object>> ADAPTER = INSTANCE.adapter(MAP_TYPE);

    public static Moshi getInstance() {
        return INSTANCE;
    }

    public static JsonAdapter<Map<String, Object>> getAdapter() {
        return ADAPTER;
    }

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