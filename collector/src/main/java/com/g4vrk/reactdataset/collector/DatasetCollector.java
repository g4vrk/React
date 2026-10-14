package com.g4vrk.reactdataset.collector;

import com.g4vrk.react.check.processor.rotation.RotationFactory;
import com.g4vrk.react.player.ReactPlayer;
import com.g4vrk.react.player.registry.PlayerRegistry;
import com.g4vrk.reactdataset.model.RecordLabel;
import com.g4vrk.reactdataset.notification.RecordingNotifier;
import com.g4vrk.reactdataset.session.RecordingSession;
import com.g4vrk.reactdataset.session.SessionSnapshot;
import com.g4vrk.reactdataset.session.manager.SessionManager;
import com.g4vrk.reactdataset.storage.CsvDatasetStorage;
import com.g4vrk.schedula.task.TickSchedule;
import com.g4vrk.schedula.task.scheduler.Scheduler;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import com.g4vrk.react.inference.InferenceModel;
import com.g4vrk.react.universe.UniverseFrame;
import com.g4vrk.react.React;

public final class DatasetCollector {
    private final Logger logger;
    private final PlayerRegistry playerRegistry;
    private final RotationFactory rotationFactory;
    private final SessionManager sessions;
    private final CsvDatasetStorage storage;
    private final Scheduler scheduler;
    private final boolean combatOnly;
    private final int configuredMaximum;
    private volatile Consumer<Path> onFileWritten = path -> {};
    private volatile BiConsumer<UUID, SessionSnapshot> onProgress = (uuid, snapshot) -> {};

    public DatasetCollector(Logger logger, PlayerRegistry playerRegistry, RotationFactory rotationFactory, SessionManager sessions, CsvDatasetStorage storage, Scheduler scheduler, boolean combatOnly, int configuredMaximum) {
        this.logger = logger;
        this.playerRegistry = playerRegistry;
        this.rotationFactory = rotationFactory;
        this.sessions = sessions;
        this.storage = storage;
        this.scheduler = scheduler;
        this.combatOnly = combatOnly;
        this.configuredMaximum = configuredMaximum;
    }

    public boolean start(Player player, RecordLabel label, int targetFrames, String source, UUID recorderUuid, RecordingNotifier notifier) {
        return start(player, label, targetFrames, source, recorderUuid, notifier,
                InferenceModel.FLOWER);
    }

    public boolean start(Player player, RecordLabel label, int targetFrames, String source,
                         UUID recorderUuid, RecordingNotifier notifier,
                         InferenceModel mode) {
        if (targetFrames <= 0 || targetFrames > this.maxFramesPerRecording()) {
            return false;
        }
        return this.sessions.start(player, recorderUuid, label, source, targetFrames, notifier == null ? RecordingNotifier.none() : notifier, mode);
    }

    public boolean start(Player player, RecordLabel label, int targetFrames, UUID recorderUuid, RecordingNotifier notifier) {
        return this.start(player, label, targetFrames, "unknown", recorderUuid, notifier);
    }

    public boolean start(Player player, RecordLabel label, int targetFrames, RecordingNotifier notifier) {
        return this.start(player, label, targetFrames, "unknown", null, notifier);
    }

    public SessionSnapshot stop(UUID uuid) {
        RecordingSession session = this.sessions.remove(uuid);
        if (session == null) {
            return null;
        }
        RecordingSession.CapturedFile remaining = session.finishAndDrain();
        if (session.mode() == InferenceModel.UNIVERSE) {
            React.INSTANCE.getUniverseService().refresh(uuid);
        }
        SessionSnapshot snapshot = session.snapshot();
        if (remaining != null) {
            scheduleFlush(session, remaining);
        }
        return snapshot;
    }

    public Collection<SessionSnapshot> active() {
        return this.sessions.active();
    }

    public SessionSnapshot snapshot(UUID uuid) {
        RecordingSession session = this.sessions.get(uuid);
        return session == null ? null : session.snapshot();
    }

    public Path datasetsDirectory() {
        return this.storage.directory();
    }

    public int framesPerFile() {
        return this.storage.framesPerFile();
    }

    public boolean combatOnly() {
        return this.combatOnly;
    }

    public int maxFilesPerRecording() {
        return this.configuredMaximum;
    }

    public int maxFramesPerRecording() {
        return this.configuredMaximum;
    }

    public void executeAsync(Runnable task) {
        this.scheduler.scheduleAsync(task, TickSchedule.instant());
    }

    public void onFileWritten(Consumer<Path> consumer) {
        this.onFileWritten = consumer == null ? path -> {} : consumer;
    }

    public void onProgress(BiConsumer<UUID, SessionSnapshot> consumer) {
        this.onProgress = consumer == null ? (uuid, snapshot) -> {} : consumer;
    }

    public void onRotation(UUID uuid, float yaw, float pitch) {
        RecordingSession session = this.sessions.get(uuid);
        if (session == null || session.finished()
                || session.mode() != InferenceModel.FLOWER) {
            return;
        }
        ReactPlayer reactPlayer = this.playerRegistry.getPlayer(uuid);
        if (reactPlayer == null) {
            return;
        }
        if (this.combatOnly && !reactPlayer.combatActivity.isActive()) {
            return;
        }
        RecordingSession.CapturedFile captured = session.capture(this.rotationFactory, yaw, pitch);
        afterCapture(uuid, session, captured);
    }

    public boolean isUniverseRecording(ReactPlayer player) {
        RecordingSession session = sessions.get(player.getUniqueId());
        return session != null && !session.finished()
                && session.mode() == InferenceModel.UNIVERSE;
    }

    public void onUniverseFrame(ReactPlayer player, UniverseFrame frame) {
        RecordingSession session = sessions.get(player.getUniqueId());
        if (session == null || session.finished()
                || session.mode() != InferenceModel.UNIVERSE
                || (combatOnly && !player.universeData.combatActivity.isActive())) {
            return;
        }
        RecordingSession.CapturedFile captured = session.capture(frame);
        afterCapture(player.getUniqueId(), session, captured);
    }

    private void afterCapture(UUID uuid, RecordingSession session, RecordingSession.CapturedFile captured) {
        SessionSnapshot snapshot = session.snapshot();
        if (captured != null || snapshot.capturedFrames() % 20 == 0) {
            this.sessions.checkpoint(session);
        }
        this.onProgress.accept(uuid, snapshot);
        if (captured == null) {
            return;
        }
        scheduleFlush(session, captured);
        if (captured.completed()) {
            this.sessions.complete(uuid, session);
            if (session.mode() == InferenceModel.UNIVERSE) {
                React.INSTANCE.getUniverseService().refresh(uuid);
            }
        }
    }

    private void scheduleFlush(RecordingSession session, RecordingSession.CapturedFile captured) {
        if (session.mode() == InferenceModel.FLOWER) {
            this.scheduler.scheduleAsync(() -> this.flush(session, captured), TickSchedule.instant());
        } else if (session.queueWrite(() -> this.flush(session, captured))) {
            this.scheduler.scheduleAsync(() -> {
                Runnable write;
                while ((write = session.nextWrite()) != null) {
                    write.run();
                }
            }, TickSchedule.instant());
        }
    }

    private void flush(RecordingSession session, RecordingSession.CapturedFile captured) {
        try {
            Path pendingPath = this.storage.write(session.safePlayerName(), captured.fileNumber(), session.label(), session.source(), captured.frames());
            this.onFileWritten.accept(pendingPath);
            Path consolidatedPath = this.storage.pathFor(session.safePlayerName(), session.source(), session.mode());
            session.notifier().onFile(consolidatedPath, captured.captured(), session.snapshot().targetFrames());
            if (captured.completed()) {
                session.notifier().onComplete(captured.captured(), session.snapshot().targetFrames());
            }
        }
        catch (Exception ex) {
            this.logger.error("Failed to write dataset for {}", (Object)session.playerName(), (Object)ex);
            session.notifier().onError();
        }
    }

    public void stopAll() {
        this.sessions.stopAll();
    }
}
