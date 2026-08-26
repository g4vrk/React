package com.g4vrk.react.storage.backend.jdbc;

import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class JdbcExecutor {

    private final DataSource dataSource;

    public JdbcExecutor(final @NotNull DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T query(final @NotNull ConnectionFunction<T> query) throws SQLException {
        try (final Connection connection = dataSource.getConnection()) {
            return query.apply(connection);
        }
    }

    public <T> T transaction(final @NotNull ConnectionFunction<T> transaction) throws SQLException {
        try (final Connection connection = dataSource.getConnection()) {
            final boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                final T result = transaction.apply(connection);
                connection.commit();
                return result;
            } catch (final Throwable throwable) {
                try {
                    connection.rollback();
                } catch (final SQLException rollbackFailure) {
                    throwable.addSuppressed(rollbackFailure);
                }
                if (throwable instanceof final SQLException sqlException) {
                    throw sqlException;
                }
                throw new SQLException("JDBC transaction failed", throwable);
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    @FunctionalInterface
    public interface ConnectionFunction<T> {
        T apply(@NotNull Connection connection) throws SQLException;
    }
}
