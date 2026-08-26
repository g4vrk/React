package com.g4vrk.react.storage;

import com.g4vrk.react.storage.backend.StorageBackend;
import com.g4vrk.react.storage.config.DatabaseSettings;
import com.g4vrk.react.storage.config.DatabaseType;
import com.g4vrk.react.storage.model.PlayerStorageData;
import com.g4vrk.react.storage.model.StorageMutation;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageManagerTest {

    @Test
    void reconnectsAndKeepsTheNewestBoundedWrites() throws Exception {
        final AtomicInteger initializations = new AtomicInteger();
        final List<StorageMutation> persisted = new CopyOnWriteArrayList<>();
        final CountDownLatch writeCompleted = new CountDownLatch(1);
        final DatabaseSettings settings = settings();
        final StorageManager manager = new StorageManager(
                LoggerFactory.getLogger("storage-test"),
                settings,
                ignored -> new FakeBackend(initializations, persisted, writeCompleted)
        );

        manager.start().handle((ignored, failure) -> null).join();

        final UUID playerUuid = UUID.randomUUID();
        for (int violations = 1; violations <= 5; violations++) {
            manager.saveViolation(playerUuid, "aim-ai", violations);
        }

        manager.whenAvailable().get(3, TimeUnit.SECONDS);
        assertTrue(writeCompleted.await(3, TimeUnit.SECONDS));
        manager.close(List.of());

        assertTrue(initializations.get() >= 2);
        assertEquals(3, persisted.size());
        assertEquals(3.0D, ((StorageMutation.Violation) persisted.get(0)).violations());
        assertEquals(5.0D, ((StorageMutation.Violation) persisted.get(2)).violations());
    }

    private DatabaseSettings settings() {
        return new DatabaseSettings(
                true,
                DatabaseType.SQLITE,
                Duration.ofDays(30),
                Duration.ofDays(1),
                1,
                16,
                3,
                16,
                Duration.ofMillis(10),
                Duration.ofMillis(10),
                Duration.ofMillis(20),
                2.0D,
                Duration.ofSeconds(2),
                null,
                null
        );
    }

    private static final class FakeBackend implements StorageBackend {

        private final AtomicInteger initializations;
        private final List<StorageMutation> persisted;
        private final CountDownLatch writeCompleted;

        private FakeBackend(
                final AtomicInteger initializations,
                final List<StorageMutation> persisted,
                final CountDownLatch writeCompleted
        ) {
            this.initializations = initializations;
            this.persisted = persisted;
            this.writeCompleted = writeCompleted;
        }

        @Override
        public void initialize() {
            if (initializations.incrementAndGet() == 1) {
                throw new IllegalStateException("offline");
            }
        }

        @Override
        public PlayerStorageData loadPlayer(final UUID playerUuid, final long historyCutoff) {
            return PlayerStorageData.EMPTY;
        }

        @Override
        public void writeBatch(final List<StorageMutation> mutations) {
            persisted.addAll(new ArrayList<>(mutations));
            writeCompleted.countDown();
        }

        @Override
        public long deleteHistoryBefore(final long cutoff) {
            return 0L;
        }

        @Override
        public void close() {
        }
    }
}
