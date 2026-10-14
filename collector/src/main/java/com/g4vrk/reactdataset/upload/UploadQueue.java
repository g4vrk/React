package com.g4vrk.reactdataset.upload;

import com.g4vrk.reactdataset.config.DatasetSettings;
import com.g4vrk.reactdataset.model.DatasetFrame;
import com.g4vrk.reactdataset.storage.CsvDatasetStorage;
import com.g4vrk.reactdataset.upload.model.UploadResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public final class UploadQueue {
    private static final Pattern DATASET_NAME = Pattern.compile("^(?<player>.+)-(?<number>\\d+)-(?<label>cheater|legit)\\.csv$", 2);
    private final Logger logger;
    private final CsvDatasetStorage storage;
    private final UploadClient client;
    private final DatasetSettings.AfterUpload afterUpload;
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private volatile boolean forbiddenReported;

    public UploadQueue(Logger logger, CsvDatasetStorage storage, UploadClient client, DatasetSettings.AfterUpload afterUpload) {
        this.logger = logger;
        this.storage = storage;
        this.client = client;
        this.afterUpload = afterUpload;
    }

    public void processPending() {
        this.storage.pending().forEach(this::process);
    }

    public void processAsync(Path path) {
        this.process(path);
    }

    public int pendingCount() {
        return this.storage.pending().size();
    }

    public String target() {
        return this.client.uploadUri().toString();
    }

    public void process(Path path) {
        String fileName = path.getFileName().toString();
        String flightKey = path.toAbsolutePath().toString();
        if (!this.inFlight.add(flightKey)) {
            return;
        }
        try {
            Matcher matcher = DATASET_NAME.matcher(fileName);
            if (!matcher.matches()) {
                return;
            }
            List<DatasetFrame> frames = this.storage.read(path);
            if (frames.isEmpty()) {
                this.logger.warn("Dataset {} is empty or invalid; moving to rejected", (Object)fileName);
                this.moveTo(path, path.getParent().getParent().resolve("rejected"));
                return;
            }
            boolean cheater = matcher.group("label").equalsIgnoreCase("cheater");
            UploadResult result = this.client.upload(matcher.group("player"), cheater, frames);
            switch (result) {
                case OK:
                case DUPLICATE: {
                    this.finish(path);
                    break;
                }
                case REJECTED: {
                    this.moveTo(path, path.getParent().getParent().resolve("rejected"));
                    break;
                }
                case FORBIDDEN: {
                    if (this.forbiddenReported) break;
                    this.forbiddenReported = true;
                    this.logger.error("Dataset upload forbidden by {}. Check allowed IP/token.", (Object)this.client.uploadUri());
                }
            }
            if (result != UploadResult.FORBIDDEN) {
                this.forbiddenReported = false;
            }
        }
        catch (Exception ex) {
            this.logger.warn("Failed to upload dataset {}", (Object)fileName, (Object)ex);
        }
        finally {
            this.inFlight.remove(flightKey);
        }
    }

    private void finish(Path path) {
        try {
            Files.deleteIfExists(path);
        }
        catch (IOException ex) {
            this.logger.warn("Failed to delete uploaded queue item {}", (Object)path.getFileName(), (Object)ex);
        }
    }

    private void moveTo(Path path, Path directory) {
        try {
            Files.createDirectories(directory, new FileAttribute[0]);
            Files.move(path, directory.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException ex) {
            this.logger.warn("Failed to move dataset {} to {}", new Object[]{path.getFileName(), directory, ex});
        }
    }
}
