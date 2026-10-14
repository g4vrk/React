package com.g4vrk.reactdataset.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Placeholders {
    private Placeholders() {
    }

    public static Map<String, String> of(Object ... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be key/value pairs");
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), String.valueOf(values[i + 1]));
        }
        return result;
    }
}
