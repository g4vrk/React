package com.g4vrk.react.storage.backend.jdbc;

import com.g4vrk.react.storage.backend.StorageBackend;
import com.g4vrk.react.storage.config.DatabaseType;
import com.g4vrk.react.storage.config.JdbcSettings;
import com.g4vrk.react.storage.config.PoolSettings;
import com.g4vrk.react.storage.model.PlayerStorageData;
import com.g4vrk.react.storage.model.StorageMutation;
import com.g4vrk.react.storage.model.StoredInference;
import com.g4vrk.react.storage.model.StoredViolation;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.g4vrk.react.storage.constant.StorageConstants.INFERENCE_TABLE;
import static com.g4vrk.react.storage.constant.StorageConstants.VIOLATIONS_TABLE;

public final class JdbcStorageBackend implements StorageBackend {

    private static final String LOAD_VIOLATIONS = "SELECT check_name, violations, updated_at FROM " + VIOLATIONS_TABLE + " WHERE player_uuid = ?";
    private static final String LOAD_INFERENCE = "SELECT check_name, recorded_at, probability, confidence FROM " + INFERENCE_TABLE + " WHERE player_uuid = ? AND recorded_at >= ? ORDER BY recorded_at ASC";
    private static final String DELETE_OLD_INFERENCE = "DELETE FROM " + INFERENCE_TABLE + " WHERE recorded_at < ?";

    private final HikariDataSource dataSource;
    private final JdbcExecutor executor;
    private final JdbcDialect dialect;

    public JdbcStorageBackend(
            final @NotNull DatabaseType type,
            final @NotNull JdbcSettings settings
    ) {
        this.dialect = JdbcDialect.forType(type);
        this.dataSource = new HikariDataSource(hikariConfig(type, settings));
        this.executor = new JdbcExecutor(dataSource);
    }

    @Override
    public void initialize() throws Exception {
        executor.transaction(connection -> {
            try (final Statement statement = connection.createStatement()) {
                for (final String sql : dialect.schemaStatements()) {
                    statement.execute(sql);
                }
            }
            return null;
        });
    }

    @Override
    public @NotNull PlayerStorageData loadPlayer(
            final @NotNull UUID playerUuid,
            final long historyCutoff
    ) throws java.sql.SQLException {
        return executor.query(connection -> new PlayerStorageData(
                loadViolations(connection, playerUuid),
                loadInference(connection, playerUuid, historyCutoff)
        ));
    }

    @Override
    public void writeBatch(final @NotNull List<StorageMutation> mutations) throws Exception {
        if (mutations.isEmpty()) {
            return;
        }

        executor.transaction(connection -> {
            try (final PreparedStatement violationStatement = connection.prepareStatement(dialect.violationUpsert());
                 final PreparedStatement inferenceStatement = connection.prepareStatement(dialect.inferenceInsert())) {
                boolean hasViolations = false;
                boolean hasInference = false;

                for (final StorageMutation mutation : mutations) {
                    if (mutation instanceof final StorageMutation.Violation violation) {
                        bindViolation(violationStatement, violation);
                        violationStatement.addBatch();
                        hasViolations = true;
                    } else if (mutation instanceof final StorageMutation.Inference inference) {
                        bindInference(inferenceStatement, inference);
                        inferenceStatement.addBatch();
                        hasInference = true;
                    }
                }

                if (hasViolations) {
                    violationStatement.executeBatch();
                }
                if (hasInference) {
                    inferenceStatement.executeBatch();
                }
            }
            return null;
        });
    }

    @Override
    public long deleteHistoryBefore(final long cutoff) throws Exception {
        return executor.query(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(DELETE_OLD_INFERENCE)) {
                statement.setLong(1, cutoff);
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private @NotNull Map<String, StoredViolation> loadViolations(
            final @NotNull Connection connection,
            final @NotNull UUID playerUuid
    ) throws java.sql.SQLException {
        final Map<String, StoredViolation> values = new LinkedHashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(LOAD_VIOLATIONS)) {
            statement.setString(1, playerUuid.toString());
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    values.put(
                            result.getString("check_name"),
                            new StoredViolation(result.getDouble("violations"), result.getLong("updated_at"))
                    );
                }
            }
        }
        return values;
    }

    private @NotNull List<StoredInference> loadInference(
            final @NotNull Connection connection,
            final @NotNull UUID playerUuid,
            final long historyCutoff
    ) throws java.sql.SQLException {
        final List<StoredInference> values = new ArrayList<>();
        try (final PreparedStatement statement = connection.prepareStatement(LOAD_INFERENCE)) {
            statement.setString(1, playerUuid.toString());
            statement.setLong(2, historyCutoff);
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    values.add(new StoredInference(
                            result.getLong("recorded_at"),
                            result.getString("check_name"),
                            result.getDouble("probability"),
                            result.getDouble("confidence")
                    ));
                }
            }
        }
        return values;
    }

    private void bindViolation(
            final @NotNull PreparedStatement statement,
            final @NotNull StorageMutation.Violation violation
    ) throws java.sql.SQLException {
        statement.setString(1, violation.playerUuid().toString());
        statement.setString(2, violation.check());
        statement.setDouble(3, violation.violations());
        statement.setLong(4, violation.updatedAt());
    }

    private void bindInference(
            final @NotNull PreparedStatement statement,
            final @NotNull StorageMutation.Inference inference
    ) throws java.sql.SQLException {
        final StoredInference value = inference.value();
        statement.setString(1, inference.id().toString());
        statement.setString(2, inference.playerUuid().toString());
        statement.setString(3, value.check());
        statement.setLong(4, value.timestamp());
        statement.setDouble(5, value.probability());
        statement.setDouble(6, value.confidence());
    }

    private static @NotNull HikariConfig hikariConfig(
            final @NotNull DatabaseType type,
            final @NotNull JdbcSettings settings
    ) {
        final PoolSettings pool = settings.pool();
        final HikariConfig config = new HikariConfig();
        config.setPoolName("React-" + type.name());
        config.setJdbcUrl(settings.jdbcUrl());
        config.setDriverClassName(settings.driverClassName());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(pool.maximumPoolSize());
        config.setMinimumIdle(Math.min(pool.minimumIdle(), pool.maximumPoolSize()));
        config.setConnectionTimeout(pool.connectionTimeout().toMillis());
        config.setValidationTimeout(pool.validationTimeout().toMillis());
        config.setIdleTimeout(pool.idleTimeout().toMillis());
        config.setMaxLifetime(pool.maxLifetime().toMillis());
        config.setAutoCommit(true);
        settings.properties().forEach(config::addDataSourceProperty);
        return config;
    }
}
