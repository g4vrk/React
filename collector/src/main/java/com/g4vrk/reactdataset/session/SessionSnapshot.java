package com.g4vrk.reactdataset.session;

import com.g4vrk.reactdataset.model.RecordLabel;
import java.util.UUID;
import com.g4vrk.react.inference.InferenceModel;

public record SessionSnapshot(UUID uniqueId, String playerName, RecordLabel label, int captured, int targetFiles, int buffered, UUID recorderUuid, String source, InferenceModel mode) {
    public SessionSnapshot(UUID uniqueId, String playerName, RecordLabel label, int capturedFrames,
                           int targetFrames, int buffered, UUID recorderUuid, String source) {
        this(uniqueId, playerName, label, capturedFrames, targetFrames, buffered, recorderUuid,
                source, InferenceModel.FLOWER);
    }
    public int capturedFrames() {
        return this.captured;
    }

    public int targetFrames() {
        return this.targetFiles;
    }
}
