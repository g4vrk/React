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
        if (frames.stream().anyMatch(frame -> frame.universe() == null)) {
            throw new IOException("Mixed dataset schemas");
        }
        Path path = pathFor(player, source, InferenceModel.UNIVERSE);
        Files.createDirectories(path.getParent());
        String existing = "";
        if (Files.exists(path)) {
            existing = Files.readString(path, StandardCharsets.UTF_8);
            if (!existing.isEmpty() && !existing.lines().findFirst().orElse("").equals(UNIVERSE_HEADER)) {
                throw new IOException("Unsupported UNIVERSE dataset header in " + path);
            }
        }
        StringBuilder chunk = new StringBuilder(UNIVERSE_HEADER).append('\n');
        StringBuilder rows = new StringBuilder();
        for (DatasetFrame frame : frames) {
            rows.append(frame.universe().toCsvRow(label.cheater())).append('\n');
        }
        String consolidated = existing.isEmpty() ? UNIVERSE_HEADER + "\n"
                : existing + (existing.endsWith("\n") || existing.endsWith("\r") ? "" : "\n");
        Path temporary = path.resolveSibling("." + path.getFileName() + ".tmp");
        Files.writeString(temporary, consolidated + rows, StandardCharsets.UTF_8);
        moveReplacing(temporary, path);
        Path pendingDir = pendingDirectory(InferenceModel.UNIVERSE);
        Files.createDirectories(pendingDir);
        Path pending = pendingDir.resolve(player + "-" + number + "-" + label.fileLabel() + ".csv");
        Files.writeString(pending, chunk.append(rows), StandardCharsets.UTF_8);
        return pending;
    }

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
