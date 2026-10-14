package com.g4vrk.reactdataset.session.manager;

import com.g4vrk.reactdataset.model.RecordLabel;
import com.g4vrk.reactdataset.notification.RecordingNotifier;
import com.g4vrk.reactdataset.session.RecordingSession;
import com.g4vrk.reactdataset.session.SessionSnapshot;
import com.g4vrk.reactdataset.session.SessionStore;
import com.g4vrk.reactdataset.storage.CsvDatasetStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import com.g4vrk.react.inference.InferenceModel;
import java.util.List;

public final class SessionManager {
    private final Map<UUID, RecordingSession> sessions = new ConcurrentHashMap<UUID, RecordingSession>();
    private final CsvDatasetStorage storage;
    private final SessionStore store;

    public SessionManager(CsvDatasetStorage storage) {
        this.storage = storage;
        this.store = new SessionStore(storage.logger(), storage.directory().resolve(".recording-sessions"));
        for (RecordingSession session : this.store.load()) {
            if (session.finished()) continue;
            this.sessions.put(session.uniqueId(), session);
        }
    }

    public boolean start(Player player, UUID recorderUuid, RecordLabel label, String source, int targetFrames, RecordingNotifier notifier) {
        return start(player, recorderUuid, label, source, targetFrames, notifier,
                InferenceModel.FLOWER);
    }

    public boolean start(Player player, UUID recorderUuid, RecordLabel label, String source,
                         int targetFrames, RecordingNotifier notifier,
                         InferenceModel mode) {
        String safe = this.storage.safePlayerName(player.getName());
        RecordingSession session = new RecordingSession(player.getUniqueId(), player.getName(), safe, label, this.storage.safeSource(source), targetFrames, this.storage.nextFreeNumber(safe, label, mode), this.storage.framesPerFile(), recorderUuid, notifier, 0, 0, List.of(), null, mode);
        if (this.sessions.putIfAbsent(player.getUniqueId(), session) != null) {
            return false;
        }
        this.store.save(session);
        return true;
    }

    public RecordingSession get(UUID uuid) {
        return this.sessions.get(uuid);
    }

    public RecordingSession remove(UUID uuid) {
        RecordingSession session = this.sessions.remove(uuid);
        if (session != null) {
            this.store.delete(uuid);
        }
        return session;
    }

    public void checkpoint(RecordingSession session) {
        if (this.sessions.get(session.uniqueId()) == session) {
            this.store.save(session);
        }
    }

    public void complete(UUID uuid, RecordingSession expected) {
        if (this.sessions.remove(uuid, expected)) {
            this.store.delete(uuid);
        }
    }

    public Collection<SessionSnapshot> active() {
        ArrayList<SessionSnapshot> result = new ArrayList<SessionSnapshot>();
        this.sessions.values().forEach(session -> result.add(session.snapshot()));
        return result;
    }

    public void stopAll() {
        this.sessions.values().forEach(this.store::save);
        this.sessions.values().forEach(RecordingSession::finish);
        this.sessions.clear();
    }
}
