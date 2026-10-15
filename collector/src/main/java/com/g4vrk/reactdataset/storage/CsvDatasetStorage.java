package com.g4vrk.reactdataset.storage;

import com.g4vrk.reactdataset.model.DatasetFrame;
import com.g4vrk.reactdataset.model.RecordLabel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import com.g4vrk.react.inference.InferenceModel;
import com.g4vrk.react.universe.UniverseFrame;
import com.g4vrk.reactdataset.session.RecordingSession;
import java.util.Properties;
import java.util.UUID;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

public final class CsvDatasetStorage {
    public static final String HEADER = "label,x,y,deltaX,deltaY,jerkX,jerkY,gcdErrorX,gcdErrorY";
    public static final String UNIVERSE_HEADER = UniverseFrame.csvHeader();
    private static final String SOURCE_HEADER = "label,source,x,y,deltaX,deltaY,jerkX,jerkY,gcdErrorX,gcdErrorY";
    private static final String OLD_HEADER = "recording,label,x,y,deltaX,deltaY,jerkX,jerkY,gcdErrorX,gcdErrorY";
    private static final String LEGACY_HEADER = "x,y,deltaX,deltaY,jerkX,jerkY,gcdErrorX,gcdErrorY";
    private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9_\\-.]+");
    private static final Pattern EDGES = Pattern.compile("^[._-]+|[._-]+$");
    private static final Pattern PENDING = Pattern.compile("^(.+)-(\\d+)-(cheater|legit)\\.csv$", 2);
    private final Logger logger;
    private final Path directory;
    private final int framesPerFile;
    private final java.util.Map<Path, Long> universeCommittedSizes = new java.util.HashMap<>();
    private final java.util.Map<UUID, Path> universeRecordingPaths = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> universePendingNumbers = new java.util.HashMap<>();

    public CsvDatasetStorage(Logger logger, Path path, int n) {
        this.logger = logger;
        this.directory = path;
        this.framesPerFile = n;
        try {
            Files.createDirectories(path, new FileAttribute[0]);
            Files.createDirectories(this.pendingDirectory(), new FileAttribute[0]);
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Cannot create datasets directory " + String.valueOf(path), iOException);
        }
    }

    public Logger logger() {
        return this.logger;
    }

    public Path directory() {
        return this.directory;
    }

    public int framesPerFile() {
        return this.framesPerFile;
    }

    public String safePlayerName(String string) {
        return CsvDatasetStorage.sanitize(string, "player", 48);
    }

    public String safeSource(String string) {
        return CsvDatasetStorage.sanitize(string, "unknown", 96);
    }

    private static String sanitize(String string, String string2, int n) {
        String string3 = UNSAFE.matcher(string == null ? string2 : string).replaceAll("_");
        if ((string3 = EDGES.matcher(string3).replaceAll("")).isBlank()) {
            string3 = string2;
        }
        return string3.length() <= n ? string3 : string3.substring(0, n);
    }

    public synchronized int nextFreeNumber(String string, RecordLabel recordLabel) {
        return nextFreeNumber(string, recordLabel, InferenceModel.FLOWER);
    }

    public synchronized int nextFreeNumber(String string, RecordLabel recordLabel,
                                           InferenceModel mode) {
        int n = 0;
        for (Path path : this.pending()) {
            if (!path.getParent().equals(pendingDirectory(mode))) continue;
            Matcher matcher = PENDING.matcher(path.getFileName().toString());
            if (!matcher.matches() || !matcher.group(1).equalsIgnoreCase(string) || !matcher.group(3).equalsIgnoreCase(recordLabel.fileLabel())) continue;
            try {
                n = Math.max(n, Integer.parseInt(matcher.group(2)));
            }
            catch (NumberFormatException numberFormatException) {}
        }
        return n + 1;
    }

    public synchronized Path write(String string, int n, RecordLabel recordLabel, String string2, List<DatasetFrame> list) throws IOException {
        if (list.isEmpty()) {
            throw new IOException("Refusing to write an empty dataset chunk");
        }
        if (list.get(0).universe() != null) {
            return writeUniverse(string, n, recordLabel, string2, list);
        }
        if (list.stream().anyMatch(frame -> frame.universe() != null)) {
            throw new IOException("Mixed dataset schemas");
        }
        Files.createDirectories(this.directory, new FileAttribute[0]);
        Path path = this.pathFor(string, string2);
        Path path2 = path.resolveSibling("." + String.valueOf(path.getFileName()) + ".tmp");
        String string3 = this.normalizedExisting(path);
        StringBuilder stringBuilder = new StringBuilder(string3.length() + list.size() * 96 + 64);
        if (string3.isEmpty()) {
            stringBuilder.append(HEADER).append('\n');
        } else {
            stringBuilder.append(string3);
            if (!string3.endsWith("\n") && !string3.endsWith("\r")) {
                stringBuilder.append('\n');
            }
        }
        int n2 = recordLabel.cheater() ? 1 : 0;
        for (DatasetFrame object2 : list) {
            stringBuilder.append(n2).append(',');
            CsvDatasetStorage.appendFrame(stringBuilder, object2);
        }
        Files.writeString(path2, (CharSequence)stringBuilder, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        CsvDatasetStorage.moveReplacing(path2, path);
        Files.createDirectories(this.pendingDirectory(), new FileAttribute[0]);
        Path path3 = this.pendingDirectory().resolve(string + "-" + n + "-" + recordLabel.fileLabel() + ".csv");
        StringBuilder stringBuilder2 = new StringBuilder(list.size() * 96 + 64).append(HEADER).append('\n');
        for (DatasetFrame datasetFrame : list) {
            stringBuilder2.append(n2).append(',');
            CsvDatasetStorage.appendFrame(stringBuilder2, datasetFrame);
        }
        Files.writeString(path3, (CharSequence)stringBuilder2, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return path3;
    }

    private Path writeUniverse(String player, int number, RecordLabel label, String source,
                               List<DatasetFrame> frames) throws IOException {
        Path journal = stageUniverseBatch(UUID.randomUUID().toString(),
                pathFor(player, source, InferenceModel.UNIVERSE),
                player + "-" + number + "-" + label.fileLabel() + ".csv", label, frames);
        return commitUniverseBatch(journal).pending();
    }

    public synchronized Path pathForRecording(String player, String source, LocalDate date, UUID recordingId) {
        Path reserved = universeRecordingPaths.get(recordingId);
        if (reserved != null) return reserved;
        String base = safePlayerName(player) + "-" + safeSource(source) + "-" + date;
        Path folder = modeDirectory(InferenceModel.UNIVERSE);
        Path reservations = folder.resolve(".recording-names");
        try {
            Files.createDirectories(reservations);
            java.util.Set<String> recovering = new java.util.HashSet<>();
            for (Path journal : universeRecovery()) {
                Properties data = new Properties();
                try (var reader = Files.newBufferedReader(journal, StandardCharsets.UTF_8)) { data.load(reader); }
                recovering.add(data.getProperty("csv"));
            }
            for (int suffix = 1; ; suffix++) {
                String name = base + (suffix == 1 ? "" : "-" + suffix) + ".csv";
                Path candidate = folder.resolve(name);
                if (Files.exists(candidate) || recovering.contains(name)) continue;
                try {
                    // Reserve names without creating an empty CSV; safe across addon reloads and writers.
                    Files.writeString(reservations.resolve(name + ".reserve"), recordingId.toString(),
                            StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    universeRecordingPaths.put(recordingId, candidate);
                    return candidate;
                } catch (java.nio.file.FileAlreadyExistsException taken) {
                    // Another recording reserved this readable name before its first batch was saved.
                }
            }
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException("Cannot reserve UNIVERSE dataset name", failure);
        }
    }

    synchronized String reserveUniversePendingName(String player, RecordLabel label) throws IOException {
        String key = (safePlayerName(player) + "-" + label.fileLabel()).toLowerCase(Locale.ROOT);
        Integer previous = universePendingNumbers.get(key);
        int number = previous == null ? nextFreeNumber(safePlayerName(player), label, InferenceModel.UNIVERSE) - 1 : previous;
        if (previous == null) {
            for (Path journal : universeRecovery()) {
                Properties data = new Properties();
                try (var reader = Files.newBufferedReader(journal, StandardCharsets.UTF_8)) { data.load(reader); }
                Matcher matcher = PENDING.matcher(data.getProperty("pending", ""));
                if (matcher.matches() && matcher.group(1).equalsIgnoreCase(safePlayerName(player))
                        && matcher.group(3).equalsIgnoreCase(label.fileLabel())) {
                    try { number = Math.max(number, Integer.parseInt(matcher.group(2))); }
                    catch (NumberFormatException previousVersion) { /* Older jobs used larger identifiers. */ }
                }
            }
        }
        number = Math.incrementExact(number);
        universePendingNumbers.put(key, number);
        return safePlayerName(player) + "-" + number + "-" + label.fileLabel() + ".csv";
    }

    private Path universeRecoveryDirectory() {
        return modeDirectory(InferenceModel.UNIVERSE).resolve(".batch-recovery");
    }

    synchronized List<Path> universeRecovery() throws IOException {
        Path recovery = universeRecoveryDirectory();
        if (!Files.isDirectory(recovery)) return List.of();
        try (var files = Files.list(recovery)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".properties"))
                    .sorted().toList();
        }
    }

    synchronized Path stageUniverseBatch(String id, Path target, String pendingName,
                                         RecordLabel label, List<DatasetFrame> frames) throws IOException {
        if (frames.size() != RecordingSession.UNIVERSE_BATCH_SIZE
                || frames.stream().anyMatch(frame -> frame.universe() == null)) {
            throw new IOException("UNIVERSE requires exactly 64 frames per batch");
        }
        Files.createDirectories(universeRecoveryDirectory());
        Path journal = universeRecoveryDirectory().resolve(id + ".properties");
        if (Files.exists(journal)) return journal;
        Properties data = new Properties();
        data.setProperty("csv", target.getFileName().toString());
        data.setProperty("pending", pendingName);
        StringBuilder rows = new StringBuilder();
        for (DatasetFrame frame : frames) rows.append(frame.universe().toCsvRow(label.cheater())).append('\n');
        data.setProperty("rows", rows.toString());
        storeUniverseJournal(journal, data);
        return journal;
    }

    synchronized SavedBatch commitUniverseBatch(Path journal) throws IOException {
        Properties data = new Properties();
        try (var reader = Files.newBufferedReader(journal, StandardCharsets.UTF_8)) {
            data.load(reader);
        }
        Path csv = safeChild(modeDirectory(InferenceModel.UNIVERSE), data.getProperty("csv"));
        Path pending = safeChild(pendingDirectory(InferenceModel.UNIVERSE), data.getProperty("pending"));
        String rows = data.getProperty("rows", "");
        if (rows.lines().count() != RecordingSession.UNIVERSE_BATCH_SIZE) {
            throw new IOException("Invalid UNIVERSE recovery batch: " + journal);
        }
        if (Files.exists(csv) && !Files.isRegularFile(csv)) {
            throw new IOException("UNIVERSE destination is not a file: " + csv);
        }
        long size = Files.exists(csv) ? Files.size(csv) : 0L;
        long offset;
        try {
            offset = Long.parseLong(data.getProperty("offset", Long.toString(size)));
        } catch (NumberFormatException invalid) {
            throw new IOException("Invalid UNIVERSE recovery offset", invalid);
        }
        if (!data.containsKey("offset")) {
            if (size > 0L && !java.util.Objects.equals(universeCommittedSizes.get(csv), size)) {
                try (var lines = Files.lines(csv, StandardCharsets.UTF_8)) {
                    var iterator = lines.iterator();
                    if (!iterator.hasNext() || !UNIVERSE_HEADER.equals(iterator.next())) {
                        throw new IOException("Unsupported UNIVERSE CSV header: " + csv);
                    }
                    long count = 0;
                    while (iterator.hasNext()) { iterator.next(); count++; }
                    if (count % RecordingSession.UNIVERSE_BATCH_SIZE != 0) {
                        throw new IOException("UNIVERSE destination has an incomplete batch: " + csv);
                    }
                }
            }
            data.setProperty("offset", Long.toString(offset));
            storeUniverseJournal(journal, data);
        }
        byte[] payload = ((offset == 0L ? UNIVERSE_HEADER + "\n" : "") + rows).getBytes(StandardCharsets.UTF_8);
        appendUniverseBatch(csv, offset, payload);
        universeCommittedSizes.put(csv, Files.size(csv));
        writeUniverseAtomic(pending, UNIVERSE_HEADER + "\n" + rows);
        Files.delete(journal);
        return new SavedBatch(csv, pending);
    }

    private static void appendUniverseBatch(Path csv, long offset, byte[] payload) throws IOException {
        try (FileChannel channel = FileChannel.open(csv, StandardOpenOption.CREATE,
                StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            long size = channel.size();
            if (offset < 0L || size < offset) throw new IOException("UNIVERSE recovery conflicts with " + csv);
            int present = (int)Math.min(payload.length, size - offset);
            ByteBuffer existing = ByteBuffer.allocate(present);
            channel.position(offset);
            while (existing.hasRemaining()) {
                if (channel.read(existing) < 0) throw new IOException("Truncated UNIVERSE CSV: " + csv);
            }
            for (int index = 0; index < present; index++) {
                if (existing.array()[index] != payload[index]) {
                    throw new IOException("UNIVERSE recovery conflicts with " + csv);
                }
            }
            if (present == payload.length) {
                channel.force(true);
                return; // This batch was already committed before a failed retry.
            }
            // Only a partial tail belonging to this journal may be rolled back; prior batches stay intact.
            channel.truncate(offset);
            channel.position(offset);
            try {
                ByteBuffer bytes = ByteBuffer.wrap(payload);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            } catch (IOException failure) {
                try { channel.truncate(offset); channel.force(true); }
                catch (IOException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        } catch (IOException failure) {
            if (offset == 0L) {
                try { if (Files.isRegularFile(csv) && Files.size(csv) == 0L) Files.delete(csv); }
                catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
    }

    private static Path safeChild(Path parent, String name) throws IOException {
        if (name == null || name.equals(".") || name.equals("..")
                || !Path.of(name).getFileName().toString().equals(name)) {
            throw new IOException("Invalid UNIVERSE recovery path");
        }
        return parent.resolve(name);
    }

    private static void storeUniverseJournal(Path path, Properties data) throws IOException {
        var text = new java.io.StringWriter();
        data.store(text, "Completed UNIVERSE batch");
        writeUniverseAtomic(path, text.toString());
    }

    private static void writeUniverseAtomic(Path path, String text) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling("." + path.getFileName() + ".tmp");
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer bytes = StandardCharsets.UTF_8.encode(text);
            while (bytes.hasRemaining()) channel.write(bytes);
            channel.force(true);
        }
        // If replacement is unsupported, retain the journal and previous CSV for retry.
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    record SavedBatch(Path csv, Path pending) {}

    public Path write(String string, int n, RecordLabel recordLabel, List<DatasetFrame> list) throws IOException {
        return this.write(string, n, recordLabel, "unknown", list);
    }

    private String normalizedExisting(Path path) throws IOException {
        if (!Files.exists(path, new LinkOption[0])) {
            return "";
        }
        List<String> list = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (list.isEmpty()) {
            return "";
        }
        String string = list.get(0).trim();
        if (HEADER.equals(string)) {
            return String.join((CharSequence)"\n", list) + "\n";
        }
        if (!SOURCE_HEADER.equals(string) && !OLD_HEADER.equals(string)) {
            throw new IOException("Unsupported existing dataset header in " + String.valueOf(path.getFileName()));
        }
        StringBuilder stringBuilder = new StringBuilder().append(HEADER).append('\n');
        for (int i = 1; i < list.size(); ++i) {
            String string2 = list.get(i).trim();
            if (string2.isEmpty()) continue;
            String[] stringArray = string2.split(",", -1);
            if (stringArray.length != 10) {
                throw new IOException("Invalid old dataset row " + (i + 1));
            }
            if (SOURCE_HEADER.equals(string)) {
                stringBuilder.append(stringArray[0]);
            } else {
                stringBuilder.append(stringArray[1]);
            }
            for (int j = 2; j < stringArray.length; ++j) {
                stringBuilder.append(',').append(stringArray[j]);
            }
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    public List<DatasetFrame> read(Path path) throws IOException {
        int n;
        int n2;
        List<String> list = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (list.size() < 2) {
            return List.of();
        }
        String string = list.get(0).trim();
        if (UNIVERSE_HEADER.equals(string)) {
            ArrayList<DatasetFrame> frames = new ArrayList<>();
            for (int row = 1; row < list.size(); row++) {
                if (list.get(row).isBlank()) continue;
                String[] parts = list.get(row).split(",", -1);
                if (parts.length != 46) return List.of();
                float[] values = new float[45];
                try {
                    for (int index = 0; index < values.length; index++) {
                        values[index] = Float.parseFloat(parts[index + 1]);
                    }
                } catch (NumberFormatException invalid) {
                    return List.of();
                }
                frames.add(DatasetFrame.from(UniverseFrame.fromFeatures(values)));
            }
            return frames;
        }
        if (HEADER.equals(string)) {
            n2 = 9;
            n = 1;
        } else if (LEGACY_HEADER.equals(string)) {
            n2 = 8;
            n = 0;
        } else if (SOURCE_HEADER.equals(string) || OLD_HEADER.equals(string)) {
            n2 = 10;
            n = 2;
        } else {
            return List.of();
        }
        ArrayList<DatasetFrame> arrayList = new ArrayList<DatasetFrame>(list.size() - 1);
        for (int i = 1; i < list.size(); ++i) {
            String string2 = list.get(i).trim();
            if (string2.isEmpty()) continue;
            String[] stringArray = string2.split(",", -1);
            if (stringArray.length != n2) {
                return List.of();
            }
            try {
                arrayList.add(new DatasetFrame(Float.parseFloat(stringArray[n]), Float.parseFloat(stringArray[n + 1]), Float.parseFloat(stringArray[n + 2]), Float.parseFloat(stringArray[n + 3]), Float.parseFloat(stringArray[n + 4]), Float.parseFloat(stringArray[n + 5]), Float.parseFloat(stringArray[n + 6]), Float.parseFloat(stringArray[n + 7])));
                continue;
            }
            catch (NumberFormatException numberFormatException) {
                return List.of();
            }
        }
        return arrayList;
    }

    public List<Path> pending() {
        ArrayList<Path> paths = new ArrayList<>();
        for (InferenceModel mode : InferenceModel.values()) {
            Path pending = pendingDirectory(mode);
            if (!Files.exists(pending)) continue;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(pending, "*.csv")) {
                for (Path path : stream) {
                    if (Files.isRegularFile(path)) paths.add(path);
                }
            } catch (IOException failure) {
                logger.warn("Unable to scan dataset upload queue", failure);
            }
        }
        return paths;
    }

    public Path modeDirectory(InferenceModel mode) {
        return mode == InferenceModel.UNIVERSE
                ? directory.resolve("universe") : directory;
    }

    public Path pathFor(String player, String source, InferenceModel mode) {
        return modeDirectory(mode).resolve(safePlayerName(player) + "-" + safeSource(source)
                + "-" + LocalDate.now() + ".csv");
    }

    private Path pendingDirectory(InferenceModel mode) {
        return modeDirectory(mode).resolve(".pending");
    }

    public Path pathFor(String string, int n, RecordLabel recordLabel) {
        return this.directory.resolve(string + "-" + String.valueOf(LocalDate.now()) + ".csv");
    }

    public Path pathFor(String string, String string2) {
        return this.directory.resolve(this.safePlayerName(string) + "-" + this.safeSource(string2) + "-" + String.valueOf(LocalDate.now()) + ".csv");
    }

    private Path pendingDirectory() {
        return this.directory.resolve(".pending");
    }

    private static void appendFrame(StringBuilder stringBuilder, DatasetFrame datasetFrame) {
        stringBuilder.append(CsvDatasetStorage.num(datasetFrame.x())).append(',').append(CsvDatasetStorage.num(datasetFrame.y())).append(',').append(CsvDatasetStorage.num(datasetFrame.deltaX())).append(',').append(CsvDatasetStorage.num(datasetFrame.deltaY())).append(',').append(CsvDatasetStorage.num(datasetFrame.jerkX())).append(',').append(CsvDatasetStorage.num(datasetFrame.jerkY())).append(',').append(CsvDatasetStorage.num(datasetFrame.gcdErrorX())).append(',').append(CsvDatasetStorage.num(datasetFrame.gcdErrorY())).append('\n');
    }

    private static void moveReplacing(Path path, Path path2) throws IOException {
        try {
            Files.move(path, path2, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
            Files.move(path, path2, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String num(float f) {
        return Float.isFinite(f) ? Float.toString(f).toLowerCase(Locale.ROOT) : "0";
    }
}
