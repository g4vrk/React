package com.g4vrk.react.storage.config;

import com.g4vrk.functionalConfiguration.Config;
import com.g4vrk.react.parse.time.TimeParser;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.g4vrk.react.storage.constant.StorageConstants.*;

public final class DatabaseSettingsFactory {

    public @NotNull DatabaseSettings create(
            final @NotNull Map<String, Config> configs,
            final @NotNull File pluginDirectory
    ) {
        final Config databaseConfig = requireConfig(configs, DATABASE_CONFIG);
        final ConfigurationNode root = databaseConfig.getRoot();
        final DatabaseType type = DatabaseType.parse(root.node("type").getString("sqlite"));
        final ConfigurationNode async = root.node("async");
        final ConfigurationNode history = root.node("history");
        final ConfigurationNode retry = async.node("retry");

        final JdbcSettings jdbcSettings;
        final MongoSettings mongoSettings;

        if (type == DatabaseType.MONGODB) {
            jdbcSettings = null;
            mongoSettings = mongo(requireConfig(configs, type.configName()).getRoot());
        } else {
            jdbcSettings = jdbc(type, requireConfig(configs, type.configName()).getRoot(), pluginDirectory);
            mongoSettings = null;
        }

        return new DatabaseSettings(
                root.node("enabled").getBoolean(true),
                type,
                positiveDuration(history.node("retention").getString(DEFAULT_RETENTION), Duration.ofDays(30)),
                positiveDuration(history.node("cleanup-interval").getString(DEFAULT_CLEANUP_INTERVAL), Duration.ofHours(1)),
                Math.max(1, async.node("executor-threads").getInt(DEFAULT_EXECUTOR_THREADS)),
                Math.max(1, async.node("executor-queue-capacity").getInt(DEFAULT_EXECUTOR_QUEUE_CAPACITY)),
                Math.max(1, async.node("pending-write-capacity").getInt(DEFAULT_PENDING_WRITE_CAPACITY)),
                Math.max(1, async.node("batch-size").getInt(DEFAULT_BATCH_SIZE)),
                positiveDuration(async.node("flush-interval").getString(DEFAULT_FLUSH_INTERVAL), Duration.ofMillis(250)),
                positiveDuration(retry.node("initial-delay").getString(DEFAULT_RETRY_INITIAL_DELAY), Duration.ofSeconds(1)),
                positiveDuration(retry.node("maximum-delay").getString(DEFAULT_RETRY_MAXIMUM_DELAY), Duration.ofMinutes(1)),
                Math.max(1.0D, retry.node("multiplier").getDouble(2.0D)),
                positiveDuration(async.node("shutdown-timeout").getString(DEFAULT_SHUTDOWN_TIMEOUT), Duration.ofSeconds(10)),
                jdbcSettings,
                mongoSettings
        );
    }

    private @NotNull JdbcSettings jdbc(
            final @NotNull DatabaseType type,
            final @NotNull ConfigurationNode root,
            final @NotNull File pluginDirectory
    ) {
        final ConfigurationNode poolNode = root.node("pool");
        final PoolSettings pool = new PoolSettings(
                Math.max(1, poolNode.node("maximum-pool-size").getInt(type == DatabaseType.SQLITE ? 1 : 10)),
                Math.max(0, poolNode.node("minimum-idle").getInt(type == DatabaseType.SQLITE ? 1 : 2)),
                positiveDuration(poolNode.node("connection-timeout").getString("10s"), Duration.ofSeconds(10)),
                positiveDuration(poolNode.node("validation-timeout").getString("5s"), Duration.ofSeconds(5)),
                positiveDuration(poolNode.node("idle-timeout").getString("10m"), Duration.ofMinutes(10)),
                positiveDuration(poolNode.node("max-lifetime").getString("30m"), Duration.ofMinutes(30))
        );

        return switch (type) {
            case SQLITE -> sqlite(root, pluginDirectory, pool);
            case H2 -> h2(root, pluginDirectory, pool);
            case MYSQL -> mysql(root, pool);
            case MONGODB -> throw new IllegalArgumentException("MongoDB is not a JDBC database");
        };
    }

    private @NotNull JdbcSettings sqlite(
            final @NotNull ConfigurationNode root,
            final @NotNull File pluginDirectory,
            final @NotNull PoolSettings pool
    ) {
        final Path file = localDatabaseFile(pluginDirectory, DatabaseType.SQLITE, root.node("file").getString("react.db"), ".db");
        final ConfigurationNode pragmas = root.node("pragmas");
        final Map<String, String> properties = new LinkedHashMap<>();
        properties.put("journal_mode", pragmas.node("journal-mode").getString("WAL"));
        properties.put("synchronous", pragmas.node("synchronous").getString("NORMAL"));
        properties.put("busy_timeout", String.valueOf(Math.max(0, pragmas.node("busy-timeout-millis").getInt(5000))));
        properties.put("foreign_keys", String.valueOf(pragmas.node("foreign-keys").getBoolean(true)));
        properties.put("cache_size", String.valueOf(pragmas.node("cache-size").getInt(-20000)));

        return new JdbcSettings("jdbc:sqlite:" + normalize(file), SQLITE_DRIVER, "", "", pool, properties);
    }

    private @NotNull JdbcSettings h2(
            final @NotNull ConfigurationNode root,
            final @NotNull File pluginDirectory,
            final @NotNull PoolSettings pool
    ) {
        final Path file = localDatabaseFile(pluginDirectory, DatabaseType.H2, root.node("file").getString("react.db"), ".db");
        final String url = "jdbc:h2:file:" + normalize(file)
                + ";MODE=" + root.node("compatibility-mode").getString("REGULAR")
                + ";AUTO_SERVER=" + root.node("auto-server").getBoolean(false)
                + ";DB_CLOSE_ON_EXIT=FALSE"
                + ";DATABASE_TO_UPPER=FALSE";

        return new JdbcSettings(
                url,
                H2_DRIVER,
                root.node("username").getString("sa"),
                root.node("password").getString(""),
                pool,
                Map.of()
        );
    }

    private @NotNull JdbcSettings mysql(
            final @NotNull ConfigurationNode root,
            final @NotNull PoolSettings pool
    ) {
        final String host = root.node("host").getString("127.0.0.1");
        final int port = Math.max(1, root.node("port").getInt(3306));
        final String database = root.node("database").getString("react");
        final ConfigurationNode propertiesNode = root.node("properties");
        final Map<String, String> properties = new LinkedHashMap<>();
        properties.put("useSSL", String.valueOf(propertiesNode.node("use-ssl").getBoolean(false)));
        properties.put("verifyServerCertificate", String.valueOf(propertiesNode.node("verify-server-certificate").getBoolean(true)));
        properties.put("allowPublicKeyRetrieval", String.valueOf(propertiesNode.node("allow-public-key-retrieval").getBoolean(false)));
        properties.put("useUnicode", "true");
        properties.put("characterEncoding", propertiesNode.node("character-encoding").getString("UTF-8"));
        properties.put("cachePrepStmts", String.valueOf(propertiesNode.node("cache-prepared-statements").getBoolean(true)));
        properties.put("prepStmtCacheSize", String.valueOf(Math.max(0, propertiesNode.node("prepared-statement-cache-size").getInt(250))));
        properties.put("prepStmtCacheSqlLimit", String.valueOf(Math.max(0, propertiesNode.node("prepared-statement-cache-sql-limit").getInt(2048))));
        properties.put("useServerPrepStmts", String.valueOf(propertiesNode.node("use-server-prepared-statements").getBoolean(true)));
        properties.put("rewriteBatchedStatements", String.valueOf(propertiesNode.node("rewrite-batched-statements").getBoolean(true)));
        properties.put("tcpKeepAlive", String.valueOf(propertiesNode.node("tcp-keep-alive").getBoolean(true)));

        return new JdbcSettings(
                "jdbc:mysql://" + host + ':' + port + '/' + database,
                MYSQL_DRIVER,
                root.node("username").getString("react"),
                root.node("password").getString("change-me"),
                pool,
                properties
        );
    }

    private @NotNull MongoSettings mongo(final @NotNull ConfigurationNode root) {
        final ConfigurationNode pool = root.node("pool");
        final ConfigurationNode timeouts = root.node("timeouts");
        return new MongoSettings(
                root.node("uri").getString(""),
                root.node("host").getString("127.0.0.1"),
                Math.max(1, root.node("port").getInt(27017)),
                root.node("database").getString("react"),
                root.node("username").getString(""),
                root.node("password").getString(""),
                root.node("authentication-database").getString("admin"),
                root.node("tls").getBoolean(false),
                root.node("retry-writes").getBoolean(true),
                Math.max(0, pool.node("minimum-size").getInt(0)),
                Math.max(1, pool.node("maximum-size").getInt(10)),
                positiveDuration(timeouts.node("connect").getString("10s"), Duration.ofSeconds(10)),
                positiveDuration(timeouts.node("socket").getString("10s"), Duration.ofSeconds(10)),
                positiveDuration(timeouts.node("server-selection").getString("10s"), Duration.ofSeconds(10)),
                positiveDuration(pool.node("max-idle-time").getString("10m"), Duration.ofMinutes(10)),
                positiveDuration(pool.node("max-life-time").getString("30m"), Duration.ofMinutes(30)),
                root.node("application-name").getString("React")
        );
    }

    private @NotNull Path localDatabaseFile(
            final @NotNull File pluginDirectory,
            final @NotNull DatabaseType type,
            final String configuredName,
            final @NotNull String extension
    ) {
        String fileName = configuredName == null || configuredName.isBlank() ? "react" + extension : configuredName.trim();
        if (!Path.of(fileName).getFileName().toString().equals(fileName)) {
            throw new IllegalArgumentException("Database file must be a file name, not a path: " + fileName);
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(extension)) {
            fileName += extension;
        }
        final Path result = pluginDirectory.toPath().resolve(DATA_DIRECTORY).resolve(type.name().toLowerCase(Locale.ROOT)).resolve(fileName).toAbsolutePath().normalize();
        try {
            Files.createDirectories(result.getParent());
        } catch (final IOException ex) {
            throw new IllegalStateException("Could not create the local database directory", ex);
        }
        return result;
    }

    private @NotNull Duration positiveDuration(final String input, final @NotNull Duration fallback) {
        final Duration parsed = TimeParser.parseDurationOrDefault(input, fallback);
        return parsed.isZero() || parsed.isNegative() ? fallback : parsed;
    }

    private @NotNull Config requireConfig(final @NotNull Map<String, Config> configs, final @NotNull String name) {
        final Config config = configs.get(name);
        if (config == null) {
            throw new IllegalStateException("Missing database configuration: " + name);
        }
        return config;
    }

    private @NotNull String normalize(final @NotNull Path path) {
        return path.toString().replace('\\', '/');
    }
}
