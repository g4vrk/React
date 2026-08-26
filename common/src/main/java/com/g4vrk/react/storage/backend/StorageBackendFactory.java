package com.g4vrk.react.storage.backend;

import com.g4vrk.react.storage.backend.jdbc.JdbcStorageBackend;
import com.g4vrk.react.storage.backend.mongo.MongoStorageBackend;
import com.g4vrk.react.storage.config.DatabaseSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class StorageBackendFactory {

    public @NotNull StorageBackend create(final @NotNull DatabaseSettings settings) {
        return switch (settings.type()) {
            case MONGODB -> new MongoStorageBackend(Objects.requireNonNull(settings.mongo()));
            case H2, MYSQL, SQLITE -> new JdbcStorageBackend(settings.type(), Objects.requireNonNull(settings.jdbc()));
        };
    }
}
