package com.g4vrk.react.storage.constant;

public final class StorageConstants {

    public static final String DATABASE_CONFIG = "database.yml";
    public static final String H2_CONFIG = "h2.yml";
    public static final String MYSQL_CONFIG = "mysql.yml";
    public static final String MONGODB_CONFIG = "mongodb.yml";
    public static final String SQLITE_CONFIG = "sqlite.yml";

    public static final String DATA_DIRECTORY = ".data";

    public static final String VIOLATIONS_TABLE = "react_violations";
    public static final String INFERENCE_TABLE = "react_inference_history";

    public static final String MONGO_VIOLATIONS_COLLECTION = "violations";
    public static final String MONGO_INFERENCE_COLLECTION = "inference_history";

    public static final String FIELD_ID = "_id";
    public static final String FIELD_PLAYER_UUID = "player_uuid";
    public static final String FIELD_CHECK = "check_name";
    public static final String FIELD_VIOLATIONS = "violations";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_PROBABILITY = "probability";
    public static final String FIELD_CONFIDENCE = "confidence";
    public static final String FIELD_UPDATED_AT = "updated_at";

    public static final int UUID_LENGTH = 36;
    public static final int CHECK_NAME_LENGTH = 64;

    public static final String DEFAULT_RETENTION = "30d";
    public static final String DEFAULT_CLEANUP_INTERVAL = "1h";
    public static final String DEFAULT_FLUSH_INTERVAL = "250ms";
    public static final String DEFAULT_RETRY_INITIAL_DELAY = "1s";
    public static final String DEFAULT_RETRY_MAXIMUM_DELAY = "1m";
    public static final String DEFAULT_SHUTDOWN_TIMEOUT = "10s";

    public static final int DEFAULT_EXECUTOR_THREADS = 2;
    public static final int DEFAULT_EXECUTOR_QUEUE_CAPACITY = 256;
    public static final int DEFAULT_PENDING_WRITE_CAPACITY = 8192;
    public static final int DEFAULT_BATCH_SIZE = 128;
    public static final long FAILURE_LOG_INTERVAL_MILLIS = 30_000L;

    public static final String H2_DRIVER = "org.h2.Driver";
    public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String SQLITE_DRIVER = "org.sqlite.JDBC";

    private StorageConstants() {
    }
}
