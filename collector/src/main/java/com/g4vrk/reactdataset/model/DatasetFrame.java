package com.g4vrk.reactdataset.model;

import com.g4vrk.react.player.model.rotation.Rotation;
import com.g4vrk.react.universe.UniverseFrame;

public record DatasetFrame(float x, float y, float deltaX, float deltaY, float jerkX, float jerkY, float gcdErrorX, float gcdErrorY, UniverseFrame universe) {
    public DatasetFrame(float x, float y, float deltaX, float deltaY, float jerkX, float jerkY, float gcdErrorX, float gcdErrorY) {
        this(x, y, deltaX, deltaY, jerkX, jerkY, gcdErrorX, gcdErrorY, null);
    }

    public static DatasetFrame from(UniverseFrame frame) {
        float[] values = frame.datasetFeatures();
        return new DatasetFrame(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], frame);
    }
    public static DatasetFrame from(Rotation rotation) {
        return new DatasetFrame(rotation.getX(), rotation.getY(), rotation.getDeltaX(), rotation.getDeltaY(), rotation.getJerkX(), rotation.getJerkY(), rotation.getGcdErrorX(), rotation.getGcdErrorY());
    }
}
