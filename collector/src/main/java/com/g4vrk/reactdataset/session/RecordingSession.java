package com.g4vrk.reactdataset.session;

import com.g4vrk.react.check.processor.rotation.RotationFactory;
import com.g4vrk.react.player.model.rotation.Rotation;
import com.g4vrk.reactdataset.model.DatasetFrame;
import com.g4vrk.reactdataset.model.RecordLabel;
import com.g4vrk.reactdataset.notification.RecordingNotifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.g4vrk.react.inference.InferenceModel;
import com.g4vrk.react.universe.UniverseFrame;
import java.util.ArrayDeque;
import java.util.Objects;

public final class RecordingSession {
    private final UUID uniqueId;
    private final String playerName;
    private final String safePlayerName;
    private final RecordLabel label;
    private final InferenceModel mode;
    private final String source;
    private final int targetFrames;
    private final int firstNumber;
    private final int framesPerChunk;
    private final UUID recorderUuid;
    private final RecordingNotifier notifier;
    private final List<DatasetFrame> buffer;
    private Rotation previous;
    private int capturedFrames;
    private int capturedChunks;
    private boolean finished;
    private final ArrayDeque<Runnable> writes = new ArrayDeque<>();
    private boolean writing;

    public RecordingSession(UUID uniqueId, String playerName, String safePlayerName, RecordLabel label, String source, int targetFrames, int firstNumber, int framesPerChunk, UUID recorderUuid, RecordingNotifier notifier) {
        this(uniqueId, playerName, safePlayerName, label, source, targetFrames, firstNumber, framesPerChunk, recorderUuid, notifier, 0, 0, List.of(), null);
    }

    public RecordingSession(UUID uniqueId, String playerName, String safePlayerName, RecordLabel label, String source, int targetFrames, int firstNumber, int framesPerChunk, UUID recorderUuid, RecordingNotifier notifier, int capturedFrames, int capturedChunks, List<DatasetFrame> buffered, DatasetFrame previousFrame) {
        this(uniqueId, playerName, safePlayerName, label, source, targetFrames, firstNumber,
                framesPerChunk, recorderUuid, notifier, capturedFrames, capturedChunks,
                buffered, previousFrame, InferenceModel.FLOWER);
    }

    public RecordingSession(UUID uniqueId, String playerName, String safePlayerName,
                            RecordLabel label, String source, int targetFrames, int firstNumber,
                            int framesPerChunk, UUID recorderUuid, RecordingNotifier notifier,
                            int capturedFrames, int capturedChunks, List<DatasetFrame> buffered,
                            DatasetFrame previousFrame, InferenceModel mode) {
        if (buffered.stream().anyMatch(frame -> (frame.universe() != null)
                != (mode == InferenceModel.UNIVERSE))) {
            throw new IllegalArgumentException("Mixed dataset schemas in checkpoint");
        }
        this.mode = Objects.requireNonNull(mode);
        this.uniqueId = uniqueId;
        this.playerName = playerName;
        this.safePlayerName = safePlayerName;
        this.label = label;
        this.source = source;
        this.targetFrames = targetFrames;
        this.firstNumber = firstNumber;
        this.framesPerChunk = framesPerChunk;
        this.recorderUuid = recorderUuid;
        this.notifier = notifier;
        this.capturedFrames = capturedFrames;
        this.capturedChunks = capturedChunks;
        this.buffer = new ArrayList<DatasetFrame>(buffered);
        this.previous = previousFrame == null ? null : RecordingSession.rotationFrom(previousFrame);
    }

    public InferenceModel mode() {
        return mode;
    }

    public synchronized CapturedFile capture(UniverseFrame frame) {
        if (finished || mode != InferenceModel.UNIVERSE) {
            return null;
        }
        return captureFrame(DatasetFrame.from(frame));
    }

    public synchronized CapturedFile capture(RotationFactory factory, float yaw, float pitch) {
        if (this.finished || mode != InferenceModel.FLOWER) {
            return null;
        }
        this.previous = factory.create(yaw, pitch, this.previous);
        return captureFrame(DatasetFrame.from(this.previous));
    }

    private CapturedFile captureFrame(DatasetFrame frame) {
        boolean completed;
        this.buffer.add(frame);
        ++this.capturedFrames;
        boolean bl = completed = this.capturedFrames >= this.targetFrames;
        if (this.buffer.size() < this.framesPerChunk && !completed) {
            return null;
        }
        return this.drain(completed);
    }

    public synchronized boolean queueWrite(Runnable write) {
        writes.addLast(write);
        if (writing) return false;
        writing = true;
        return true;
    }

    public synchronized Runnable nextWrite() {
        Runnable write = writes.pollFirst();
        if (write == null) writing = false;
        return write;
    }

    public synchronized CapturedFile finishAndDrain() {
        if (this.finished && this.buffer.isEmpty()) {
            return null;
        }
        this.finished = true;
        return this.buffer.isEmpty() ? null : this.drain(false);
    }

    private CapturedFile drain(boolean completed) {
        List<DatasetFrame> frames = List.copyOf(this.buffer);
        this.buffer.clear();
        int fileNumber = this.firstNumber + this.capturedChunks++;
        if (completed) {
            this.finished = true;
        }
        return new CapturedFile(fileNumber, frames, this.capturedFrames, completed);
    }

    public synchronized SessionSnapshot snapshot() {
        return new SessionSnapshot(this.uniqueId, this.playerName, this.label, this.capturedFrames, this.targetFrames, this.buffer.size(), this.recorderUuid, this.source, this.mode);
    }

    public synchronized List<DatasetFrame> bufferedFrames() {
        return List.copyOf(this.buffer);
    }

    public synchronized DatasetFrame previousFrame() {
        return this.previous == null ? null : DatasetFrame.from(this.previous);
    }

    public synchronized int capturedChunks() {
        return this.capturedChunks;
    }

    public synchronized boolean finished() {
        return this.finished;
    }

    public synchronized void finish() {
        this.finished = true;
    }

    public UUID uniqueId() {
        return this.uniqueId;
    }

    public String playerName() {
        return this.playerName;
    }

    public String safePlayerName() {
        return this.safePlayerName;
    }

    public RecordLabel label() {
        return this.label;
    }

    public String source() {
        return this.source;
    }

    public UUID recorderUuid() {
        return this.recorderUuid;
    }

    public RecordingNotifier notifier() {
        return this.notifier;
    }

    public int firstNumber() {
        return this.firstNumber;
    }

    public int framesPerChunk() {
        return this.framesPerChunk;
    }

    private static Rotation rotationFrom(DatasetFrame frame) {
        return new Rotation(frame.y(), frame.x(), frame.deltaY(), frame.deltaX(), frame.jerkY(), frame.jerkX(), frame.gcdErrorY(), frame.gcdErrorX());
    }

    public record CapturedFile(int fileNumber, List<DatasetFrame> frames, int captured, boolean completed) {
    }
}
