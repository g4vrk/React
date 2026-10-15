package com.g4vrk.react.universe;

import com.google.flatbuffers.FlatBufferBuilder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** UNIVERSE (model id 2) GAIQ request and single-item batch framing. */
public final class UniverseProtocol {
    private UniverseProtocol() {
    }

    public static byte[] encodeRequest(UniverseFrame[] frames) {
        float[] features = new float[frames.length * UniverseFrame.INFERENCE_FEATURE_COUNT];
        for (int frameIndex = 0; frameIndex < frames.length; frameIndex++) {
            float[] inputs = frames[frameIndex].inferenceFeatures();
            System.arraycopy(inputs, 0, features, frameIndex * inputs.length, inputs.length);
        }
        FlatBufferBuilder builder = new FlatBufferBuilder(64 + features.length * 4);
        builder.startVector(4, features.length, 4);
        for (int index = features.length - 1; index >= 0; index--) {
            builder.addFloat(features[index]);
        }
        int featuresOffset = builder.endVector();
        builder.startTable(4);
        builder.addOffset(2, featuresOffset, 0);
        builder.addInt(1, frames.length, 0);
        builder.addInt(3, 2, 0);
        int requestOffset = builder.endTable();
        builder.finish(requestOffset, "GAIQ");
        return builder.sizedByteArray();
    }

    public static byte[] encodeBatch(UniverseFrame[] frames) {
        byte[] payload = encodeRequest(frames);
        return ByteBuffer.allocate(2 + 4 + payload.length).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) 1).putInt(payload.length).put(payload).array();
    }

    public static double decodeResponse(byte[] body) {
        if (body.length < 14 || body[0] != 'G' || body[1] != 'A'
                || body[2] != 'I' || body[3] != 'B') {
            throw new IllegalArgumentException("Invalid UNIVERSE response; expected GAIB");
        }
        ByteBuffer buffer = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(4);
        if (Short.toUnsignedInt(buffer.getShort()) != 1) {
            throw new IllegalArgumentException("UNIVERSE response item count must be 1");
        }
        double probability = buffer.getDouble();
        // Normalize non-finite values in UNIVERSE binary responses.
        if (!Double.isFinite(probability)) {
            probability = 0.0;
        }
        return Math.max(0.0, Math.min(1.0, probability));
    }
}
