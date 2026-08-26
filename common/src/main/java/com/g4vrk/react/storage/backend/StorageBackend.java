package com.g4vrk.react.storage.backend;

import com.g4vrk.react.storage.model.PlayerStorageData;
import com.g4vrk.react.storage.model.StorageMutation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public interface StorageBackend extends AutoCloseable {

    void initialize() throws Exception;

    @NotNull PlayerStorageData loadPlayer(@NotNull UUID playerUuid, long historyCutoff) throws Exception;

    void writeBatch(@NotNull List<StorageMutation> mutations) throws Exception;

    long deleteHistoryBefore(long cutoff) throws Exception;

    @Override
    void close() throws Exception;
}
