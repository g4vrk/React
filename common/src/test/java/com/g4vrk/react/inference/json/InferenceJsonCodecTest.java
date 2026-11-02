package com.g4vrk.react.inference.json;

import com.g4vrk.react.player.model.rotation.Rotation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InferenceJsonCodecTest {

    @Test
    void writesInferenceRequestWithRotationFrames() {
        final Rotation rotation = new Rotation(
                1.0F,
                2.0F,
                3.0F,
                4.0F,
                5.0F,
                6.0F,
                7.0F,
                8.0F
        );

        final Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", "Steve");
        request.put("frames", new Rotation[]{rotation});

        assertEquals(
                "{\"name\":\"Steve\",\"frames\":[{\"y\":1.0,\"x\":2.0,\"deltaY\":3.0,\"deltaX\":4.0,"
                        + "\"jerkY\":5.0,\"jerkX\":6.0,\"gcdErrorY\":7.0,\"gcdErrorX\":8.0}]}",
                InferenceJsonCodec.writeObject(request)
        );
    }

    @Test
    void escapesRequestStrings() {
        final String json = InferenceJsonCodec.writeObject(
                Map.of("name", "Steve \"The Brave\"\\\n")
        );

        assertTrue(json.contains("Steve \\\"The Brave\\\"\\\\\\n"));
    }

    @Test
    void readsConfiguredNumberFieldAndSkipsOtherValues() {
        final String response =
                "{\"metadata\":{\"model\":\"aim\",\"labels\":[1,true,null]},"
                        + "\"cheat_probability\":0.875,\"accepted\":true}";

        assertEquals(0.875D, InferenceJsonCodec.readNumberField(response, "cheat_probability"));
    }

    @Test
    void returnsNullWhenConfiguredFieldIsNotNumericOrMissing() {
        assertNull(InferenceJsonCodec.readNumberField("{\"cheat_probability\":\"0.5\"}", "cheat_probability"));
        assertNull(InferenceJsonCodec.readNumberField("{\"accepted\":true}", "cheat_probability"));
    }

    @Test
    void rejectsMalformedJsonAndTrailingContent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InferenceJsonCodec.readNumberField("{\"probability\":}", "probability")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InferenceJsonCodec.readNumberField("[1,2,3]", "probability")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> InferenceJsonCodec.readNumberField("{\"probability\":0.5} trailing", "probability")
        );
    }

    @Test
    void rejectsNonFiniteNumbers() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InferenceJsonCodec.writeObject(Map.of("value", Double.NaN))
        );
    }
}
