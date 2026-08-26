package com.g4vrk.react.storage.backend.jdbc;

import com.g4vrk.react.storage.config.DatabaseType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.g4vrk.react.storage.constant.StorageConstants.INFERENCE_TABLE;
import static com.g4vrk.react.storage.constant.StorageConstants.VIOLATIONS_TABLE;

public enum JdbcDialect {
    SQLITE(
            List.of(
                    "CREATE TABLE IF NOT EXISTS " + VIOLATIONS_TABLE + " (player_uuid VARCHAR(36) NOT NULL, check_name VARCHAR(64) NOT NULL, violations REAL NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (player_uuid, check_name))",
                    "CREATE TABLE IF NOT EXISTS " + INFERENCE_TABLE + " (id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, check_name VARCHAR(64) NOT NULL, recorded_at BIGINT NOT NULL, probability REAL NOT NULL, confidence REAL NOT NULL)",
                    "CREATE INDEX IF NOT EXISTS react_inference_player_time_idx ON " + INFERENCE_TABLE + " (player_uuid, recorded_at)",
                    "CREATE INDEX IF NOT EXISTS react_inference_time_idx ON " + INFERENCE_TABLE + " (recorded_at)"
            ),
            "INSERT INTO " + VIOLATIONS_TABLE + " (player_uuid, check_name, violations, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(player_uuid, check_name) DO UPDATE SET violations = excluded.violations, updated_at = excluded.updated_at WHERE excluded.updated_at >= " + VIOLATIONS_TABLE + ".updated_at",
            "INSERT OR IGNORE INTO " + INFERENCE_TABLE + " (id, player_uuid, check_name, recorded_at, probability, confidence) VALUES (?, ?, ?, ?, ?, ?)"
    ),
    H2(
            List.of(
                    "CREATE TABLE IF NOT EXISTS " + VIOLATIONS_TABLE + " (player_uuid VARCHAR(36) NOT NULL, check_name VARCHAR(64) NOT NULL, violations DOUBLE PRECISION NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (player_uuid, check_name))",
                    "CREATE TABLE IF NOT EXISTS " + INFERENCE_TABLE + " (id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, check_name VARCHAR(64) NOT NULL, recorded_at BIGINT NOT NULL, probability DOUBLE PRECISION NOT NULL, confidence DOUBLE PRECISION NOT NULL)",
                    "CREATE INDEX IF NOT EXISTS react_inference_player_time_idx ON " + INFERENCE_TABLE + " (player_uuid, recorded_at)",
                    "CREATE INDEX IF NOT EXISTS react_inference_time_idx ON " + INFERENCE_TABLE + " (recorded_at)"
            ),
            "MERGE INTO " + VIOLATIONS_TABLE + " (player_uuid, check_name, violations, updated_at) KEY (player_uuid, check_name) VALUES (?, ?, ?, ?)",
            "MERGE INTO " + INFERENCE_TABLE + " (id, player_uuid, check_name, recorded_at, probability, confidence) KEY (id) VALUES (?, ?, ?, ?, ?, ?)"
    ),
    MYSQL(
            List.of(
                    "CREATE TABLE IF NOT EXISTS " + VIOLATIONS_TABLE + " (player_uuid VARCHAR(36) NOT NULL, check_name VARCHAR(64) NOT NULL, violations DOUBLE NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (player_uuid, check_name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                    "CREATE TABLE IF NOT EXISTS " + INFERENCE_TABLE + " (id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, check_name VARCHAR(64) NOT NULL, recorded_at BIGINT NOT NULL, probability DOUBLE NOT NULL, confidence DOUBLE NOT NULL, INDEX react_inference_player_time_idx (player_uuid, recorded_at), INDEX react_inference_time_idx (recorded_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            ),
            "INSERT INTO " + VIOLATIONS_TABLE + " (player_uuid, check_name, violations, updated_at) VALUES (?, ?, ?, ?) AS incoming ON DUPLICATE KEY UPDATE violations = IF(incoming.updated_at >= " + VIOLATIONS_TABLE + ".updated_at, incoming.violations, " + VIOLATIONS_TABLE + ".violations), updated_at = GREATEST(" + VIOLATIONS_TABLE + ".updated_at, incoming.updated_at)",
            "INSERT INTO " + INFERENCE_TABLE + " (id, player_uuid, check_name, recorded_at, probability, confidence) VALUES (?, ?, ?, ?, ?, ?) AS incoming ON DUPLICATE KEY UPDATE id = " + INFERENCE_TABLE + ".id"
    );

    private final List<String> schemaStatements;
    private final String violationUpsert;
    private final String inferenceInsert;

    JdbcDialect(
            final @NotNull List<String> schemaStatements,
            final @NotNull String violationUpsert,
            final @NotNull String inferenceInsert
    ) {
        this.schemaStatements = schemaStatements;
        this.violationUpsert = violationUpsert;
        this.inferenceInsert = inferenceInsert;
    }

    public static @NotNull JdbcDialect forType(final @NotNull DatabaseType type) {
        return switch (type) {
            case SQLITE -> SQLITE;
            case H2 -> H2;
            case MYSQL -> MYSQL;
            case MONGODB -> throw new IllegalArgumentException("MongoDB has no JDBC dialect");
        };
    }

    public @NotNull List<String> schemaStatements() {
        return schemaStatements;
    }

    public @NotNull String violationUpsert() {
        return violationUpsert;
    }

    public @NotNull String inferenceInsert() {
        return inferenceInsert;
    }
}
