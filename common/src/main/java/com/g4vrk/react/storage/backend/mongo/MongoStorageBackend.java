package com.g4vrk.react.storage.backend.mongo;

import com.g4vrk.react.storage.backend.StorageBackend;
import com.g4vrk.react.storage.config.MongoSettings;
import com.g4vrk.react.storage.model.PlayerStorageData;
import com.g4vrk.react.storage.model.StorageMutation;
import com.g4vrk.react.storage.model.StoredInference;
import com.g4vrk.react.storage.model.StoredViolation;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.g4vrk.react.storage.constant.StorageConstants.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;

public final class MongoStorageBackend implements StorageBackend {

    private final MongoClient client;
    private final MongoDatabase database;
    private final MongoCollection<Document> violations;
    private final MongoCollection<Document> inference;

    public MongoStorageBackend(final @NotNull MongoSettings settings) {
        this.client = MongoClients.create(clientSettings(settings));
        this.database = client.getDatabase(settings.database());
        this.violations = database.getCollection(MONGO_VIOLATIONS_COLLECTION);
        this.inference = database.getCollection(MONGO_INFERENCE_COLLECTION);
    }

    @Override
    public void initialize() {
        database.runCommand(new Document("ping", 1));
        violations.createIndex(Indexes.ascending(FIELD_PLAYER_UUID));
        inference.createIndex(Indexes.compoundIndex(Indexes.ascending(FIELD_PLAYER_UUID), Indexes.ascending(FIELD_TIMESTAMP)));
        inference.createIndex(Indexes.ascending(FIELD_TIMESTAMP));
    }

    @Override
    public @NotNull PlayerStorageData loadPlayer(
            final @NotNull UUID playerUuid,
            final long historyCutoff
    ) {
        final String uuid = playerUuid.toString();
        final Map<String, StoredViolation> loadedViolations = new java.util.LinkedHashMap<>();
        for (final Document document : violations.find(eq(FIELD_PLAYER_UUID, uuid))) {
            loadedViolations.put(
                    document.getString(FIELD_CHECK),
                    new StoredViolation(number(document, FIELD_VIOLATIONS), longNumber(document, FIELD_UPDATED_AT))
            );
        }

        final List<StoredInference> history = new ArrayList<>();
        for (final Document document : inference
                .find(and(eq(FIELD_PLAYER_UUID, uuid), gte(FIELD_TIMESTAMP, historyCutoff)))
                .sort(ascending(FIELD_TIMESTAMP))) {
            history.add(new StoredInference(
                    document.getLong(FIELD_TIMESTAMP),
                    document.getString(FIELD_CHECK),
                    number(document, FIELD_PROBABILITY),
                    number(document, FIELD_CONFIDENCE)
            ));
        }
        return new PlayerStorageData(loadedViolations, history);
    }

    @Override
    public void writeBatch(final @NotNull List<StorageMutation> mutations) {
        if (mutations.isEmpty()) {
            return;
        }

        final List<WriteModel<Document>> violationWrites = new ArrayList<>();
        final List<WriteModel<Document>> inferenceWrites = new ArrayList<>();
        final ReplaceOptions upsert = new ReplaceOptions().upsert(true);

        for (final StorageMutation mutation : mutations) {
            if (mutation instanceof final StorageMutation.Violation violation) {
                final String id = violation.playerUuid() + ":" + violation.check();
                final Document currentTimestamp = new Document("$ifNull", List.of('$' + FIELD_UPDATED_AT, Long.MIN_VALUE));
                final Document isLatest = new Document("$gte", List.of(violation.updatedAt(), currentTimestamp));
                final Document fields = new Document(FIELD_PLAYER_UUID, violation.playerUuid().toString())
                        .append(FIELD_CHECK, violation.check())
                        .append(FIELD_VIOLATIONS, new Document("$cond", List.of(
                                isLatest,
                                violation.violations(),
                                new Document("$ifNull", List.of('$' + FIELD_VIOLATIONS, 0.0D))
                        )))
                        .append(FIELD_UPDATED_AT, new Document("$max", List.of(currentTimestamp, violation.updatedAt())));
                violationWrites.add(new UpdateOneModel<>(
                        eq(FIELD_ID, id),
                        List.of(new Document("$set", fields)),
                        new UpdateOptions().upsert(true)
                ));
            } else if (mutation instanceof final StorageMutation.Inference stored) {
                final StoredInference value = stored.value();
                final Document document = new Document(FIELD_ID, stored.id().toString())
                        .append(FIELD_PLAYER_UUID, stored.playerUuid().toString())
                        .append(FIELD_CHECK, value.check())
                        .append(FIELD_TIMESTAMP, value.timestamp())
                        .append(FIELD_PROBABILITY, value.probability())
                        .append(FIELD_CONFIDENCE, value.confidence());
                inferenceWrites.add(new ReplaceOneModel<>(eq(FIELD_ID, stored.id().toString()), document, upsert));
            }
        }

        if (!violationWrites.isEmpty()) {
            violations.bulkWrite(violationWrites, new com.mongodb.client.model.BulkWriteOptions().ordered(false));
        }
        if (!inferenceWrites.isEmpty()) {
            inference.bulkWrite(inferenceWrites, new com.mongodb.client.model.BulkWriteOptions().ordered(false));
        }
    }

    @Override
    public long deleteHistoryBefore(final long cutoff) {
        return inference.deleteMany(lt(FIELD_TIMESTAMP, cutoff)).getDeletedCount();
    }

    @Override
    public void close() {
        client.close();
    }

    private static double number(final @NotNull Document document, final @NotNull String field) {
        final Number value = document.get(field, Number.class);
        return value == null ? 0.0D : value.doubleValue();
    }

    private static long longNumber(final @NotNull Document document, final @NotNull String field) {
        final Number value = document.get(field, Number.class);
        return value == null ? 0L : value.longValue();
    }

    private static @NotNull MongoClientSettings clientSettings(final @NotNull MongoSettings settings) {
        final MongoClientSettings.Builder builder = MongoClientSettings.builder();

        if (!settings.uri().isBlank()) {
            builder.applyConnectionString(new ConnectionString(settings.uri()));
        } else {
            builder.applyToClusterSettings(cluster -> cluster.hosts(List.of(new ServerAddress(settings.host(), settings.port()))));
            if (!settings.username().isBlank()) {
                builder.credential(MongoCredential.createCredential(
                        settings.username(),
                        settings.authenticationDatabase(),
                        settings.password().toCharArray()
                ));
            }
        }

        builder.applicationName(settings.applicationName())
                .retryWrites(settings.retryWrites())
                .applyToSslSettings(ssl -> ssl.enabled(settings.tls()))
                .applyToConnectionPoolSettings(pool -> pool
                        .minSize(Math.min(settings.minimumPoolSize(), settings.maximumPoolSize()))
                        .maxSize(settings.maximumPoolSize())
                        .maxConnectionIdleTime(settings.maxConnectionIdleTime().toMillis(), TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(settings.maxConnectionLifeTime().toMillis(), TimeUnit.MILLISECONDS))
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(Math.toIntExact(settings.connectTimeout().toMillis()), TimeUnit.MILLISECONDS)
                        .readTimeout(Math.toIntExact(settings.socketTimeout().toMillis()), TimeUnit.MILLISECONDS))
                .applyToClusterSettings(cluster -> cluster
                        .serverSelectionTimeout(settings.serverSelectionTimeout().toMillis(), TimeUnit.MILLISECONDS));

        return builder.build();
    }
}
