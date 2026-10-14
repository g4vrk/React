package com.g4vrk.reactdataset.session;

import com.g4vrk.reactdataset.model.DatasetFrame;
import com.g4vrk.reactdataset.model.RecordLabel;
import com.g4vrk.reactdataset.notification.RecordingNotifier;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;
import com.g4vrk.react.inference.InferenceModel;
import com.g4vrk.react.universe.UniverseFrame;

public final class SessionStore {
    private final Logger logger;
    private final Path directory;

    public SessionStore(Logger logger, Path directory) {
        this.logger = logger;
        this.directory = directory;
        try {
            Files.createDirectories(directory, new FileAttribute[0]);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Cannot create recording session directory " + String.valueOf(directory), ex);
        }
    }

    public synchronized void save(RecordingSession session) {
        Properties properties = new Properties();
        SessionSnapshot snapshot = session.snapshot();
        properties.setProperty("uuid", snapshot.uniqueId().toString());
        properties.setProperty("playerName", snapshot.playerName());
        properties.setProperty("safePlayerName", session.safePlayerName());
        properties.setProperty("label", snapshot.label().name());
        properties.setProperty("source", snapshot.source());
        properties.setProperty("mode", session.mode().name());
        properties.setProperty("targetFrames", Integer.toString(snapshot.targetFrames()));
        properties.setProperty("capturedFrames", Integer.toString(snapshot.capturedFrames()));
        properties.setProperty("capturedChunks", Integer.toString(session.capturedChunks()));
        properties.setProperty("firstNumber", Integer.toString(session.firstNumber()));
        properties.setProperty("framesPerChunk", Integer.toString(session.framesPerChunk()));
        properties.setProperty("recorderUuid", snapshot.recorderUuid() == null ? "" : snapshot.recorderUuid().toString());
        List<DatasetFrame> buffered = session.bufferedFrames();
        properties.setProperty("bufferCount", Integer.toString(buffered.size()));
        for (int i = 0; i < buffered.size(); ++i) {
            properties.setProperty("buffer." + i, SessionStore.encode(buffered.get(i)));
        }
        DatasetFrame previous = session.previousFrame();
        if (previous != null) {
            properties.setProperty("previous", SessionStore.encode(previous));
        }
        Path target = this.path(snapshot.uniqueId());
        Path temporary = target.resolveSibling("." + String.valueOf(target.getFileName()) + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);){
            properties.store(writer, "ReactDatasetCollector active recording");
        }
        catch (IOException ex) {
            this.logger.error("Failed to checkpoint recording session for {}", (Object)snapshot.playerName(), (Object)ex);
            return;
        }
        try {
            SessionStore.moveReplacing(temporary, target);
        }
        catch (IOException ex) {
            this.logger.error("Failed to activate recording checkpoint for {}", (Object)snapshot.playerName(), (Object)ex);
        }
    }

    public synchronized void delete(UUID uuid) {
        try {
            Files.deleteIfExists(this.path(uuid));
        }
        catch (IOException ex) {
            this.logger.warn("Failed to delete completed recording checkpoint {}", (Object)uuid, (Object)ex);
        }
    }

    public synchronized List<RecordingSession> load() {
        ArrayList<RecordingSession> restored = new ArrayList<RecordingSession>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.directory, "*.properties");){
            for (Path path : stream) {
                try {
                    restored.add(this.read(path));
                }
                catch (Exception ex) {
                    this.logger.error("Ignoring invalid recording checkpoint {}", (Object)path.getFileName(), (Object)ex);
                }
            }
        }
        catch (IOException ex) {
            this.logger.error("Failed to load recording checkpoints from {}", (Object)this.directory, (Object)ex);
        }
        return restored;
    }

    private RecordingSession read(Path path) throws IOException {
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            properties.load(reader);
        }
        UUID uuid = UUID.fromString(SessionStore.required(properties, "uuid"));
        String recorder = properties.getProperty("recorderUuid", "").trim();
        int bufferCount = Integer.parseInt(SessionStore.required(properties, "bufferCount"));
        ArrayList<DatasetFrame> buffer = new ArrayList<DatasetFrame>(bufferCount);
        for (int i = 0; i < bufferCount; ++i) {
            buffer.add(SessionStore.decode(SessionStore.required(properties, "buffer." + i)));
        }
        String encodedPrevious = properties.getProperty("previous", "").trim();
        DatasetFrame previous = encodedPrevious.isEmpty() ? null : SessionStore.decode(encodedPrevious);
        RecordingSession session = new RecordingSession(uuid, SessionStore.required(properties, "playerName"), SessionStore.required(properties, "safePlayerName"), RecordLabel.valueOf(SessionStore.required(properties, "label")), SessionStore.required(properties, "source"), Integer.parseInt(SessionStore.required(properties, "targetFrames")), Integer.parseInt(SessionStore.required(properties, "firstNumber")), Integer.parseInt(SessionStore.required(properties, "framesPerChunk")), recorder.isEmpty() ? null : UUID.fromString(recorder), RecordingNotifier.none(), Integer.parseInt(SessionStore.required(properties, "capturedFrames")), Integer.parseInt(SessionStore.required(properties, "capturedChunks")), buffer, previous, InferenceModel.parse(properties.getProperty("mode", "FLOWER")));
        return session;
    }

    private Path path(UUID uuid) {
        return this.directory.resolve(String.valueOf(uuid) + ".properties");
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing property " + key);
        }
        return value;
    }

    private static String encode(DatasetFrame frame) {
        if (frame.universe() != null) {
            StringBuilder values = new StringBuilder();
            for (float value : frame.universe().datasetFeatures()) {
                if (values.length() > 0) values.append(',');
                values.append(Float.toString(value));
            }
            return values.toString();
        }
        return frame.x() + "," + frame.y() + "," + frame.deltaX() + "," + frame.deltaY() + "," + frame.jerkX() + "," + frame.jerkY() + "," + frame.gcdErrorX() + "," + frame.gcdErrorY();
    }

    private static DatasetFrame decode(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length == UniverseFrame.DATASET_FEATURE_COUNT) {
            float[] values = new float[parts.length];
            for (int index = 0; index < values.length; index++) {
                values[index] = Float.parseFloat(parts[index]);
            }
            return DatasetFrame.from(UniverseFrame.fromFeatures(values));
        }
        if (parts.length != 8) {
            throw new IllegalArgumentException("Invalid persisted frame");
        }
        return new DatasetFrame(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]), Float.parseFloat(parts[6]), Float.parseFloat(parts[7]));
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
