package com.g4vrk.reactdataset.session;

import com.g4vrk.reactdataset.model.RecordLabel;
import java.util.UUID;
import com.g4vrk.react.inference.InferenceModel;

public record SessionSnapshot(UUID uniqueId, String playerName, RecordLabel label, int captured, int targetFiles, int buffered, UUID recorderUuid, String source, InferenceModel mode, int savedFrames, UUID recordingId) {
    public SessionSnapshot(UUID uniqueId, String playerName, RecordLabel label, int captured,
                           int targetFiles, int buffered, UUID recorderUuid, String source,
                           InferenceModel mode) {
        this(uniqueId, playerName, label, captured, targetFiles, buffered, recorderUuid, source,
                mode, mode == InferenceModel.FLOWER ? captured : 0, null);
    }

    public int completedBatches() {
        return mode == InferenceModel.UNIVERSE ? captured / RecordingSession.UNIVERSE_BATCH_SIZE : 0;
    }

    public int savedBatches() {
        return mode == InferenceModel.UNIVERSE ? savedFrames / RecordingSession.UNIVERSE_BATCH_SIZE : 0;
    }

    public int targetBatches() {
        return mode == InferenceModel.UNIVERSE ? targetFrames() / RecordingSession.UNIVERSE_BATCH_SIZE : 0;
    }

    public SessionSnapshot withBuffered(int count) {
        return new SessionSnapshot(uniqueId, playerName, label, captured, targetFiles, count,
                recorderUuid, source, mode, savedFrames, recordingId);
    }
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
