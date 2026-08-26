package com.g4vrk.react.storage.backend.jdbc;

import com.g4vrk.react.storage.config.DatabaseType;
import com.g4vrk.react.storage.config.JdbcSettings;
import com.g4vrk.react.storage.config.PoolSettings;
import com.g4vrk.react.storage.model.PlayerStorageData;
import com.g4vrk.react.storage.model.StorageMutation;
import com.g4vrk.react.storage.model.StoredInference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcStorageBackendTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void sqlitePersistsViolationsAndUnlimitedHistory() throws Exception {
        final JdbcSettings settings = settings(
                "jdbc:sqlite:" + temporaryDirectory.resolve("react.db").toString().replace('\\', '/'),
                "org.sqlite.JDBC",
                "",
                Map.of("journal_mode", "WAL", "busy_timeout", "5000")
        );

        verifyBackend(DatabaseType.SQLITE, settings);
    }

    @Test
    void h2PersistsViolationsAndUnlimitedHistory() throws Exception {
        final JdbcSettings settings = settings(
                "jdbc:h2:file:" + temporaryDirectory.resolve("react.db").toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_UPPER=FALSE",
                "org.h2.Driver",
                "sa",
                Map.of()
        );

        verifyBackend(DatabaseType.H2, settings);
    }

    private void verifyBackend(
            final DatabaseType type,
            final JdbcSettings settings
    ) throws Exception {
        final UUID playerUuid = UUID.randomUUID();
        final long baseTimestamp = System.currentTimeMillis() - 1_000L;
        final List<StorageMutation> mutations = new ArrayList<>();
        mutations.add(new StorageMutation.Violation(playerUuid, "aim-ai", 4.5D, baseTimestamp));

        for (int index = 0; index < 512; index++) {
            mutations.add(new StorageMutation.Inference(
                    playerUuid,
                    UUID.randomUUID(),
                    new StoredInference(baseTimestamp + index, "aim-ai", index / 512.0D, 0.9D)
            ));
        }

        try (final JdbcStorageBackend backend = new JdbcStorageBackend(type, settings)) {
            backend.initialize();
            backend.writeBatch(mutations);

            final PlayerStorageData loaded = backend.loadPlayer(playerUuid, baseTimestamp);
            assertEquals(4.5D, loaded.violations().get("aim-ai").value());
            assertEquals(512, loaded.inferenceHistory().size());

            assertEquals(256, backend.deleteHistoryBefore(baseTimestamp + 256));
            assertEquals(256, backend.loadPlayer(playerUuid, baseTimestamp).inferenceHistory().size());
        }
    }

    private JdbcSettings settings(
            final String url,
            final String driver,
            final String username,
            final Map<String, String> properties
    ) {
        return new JdbcSettings(
                url,
                driver,
                username,
                "",
                new PoolSettings(
                        1,
                        1,
                        Duration.ofSeconds(10),
                        Duration.ofSeconds(5),
                        Duration.ofMinutes(10),
                        Duration.ofMinutes(30)
                ),
                properties
        );
    }
}
