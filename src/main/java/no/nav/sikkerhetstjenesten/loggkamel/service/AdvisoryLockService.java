package no.nav.sikkerhetstjenesten.loggkamel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AdvisoryLockService {

    private static final Logger log = LoggerFactory.getLogger(AdvisoryLockService.class);

    // Use a transaction-scoped lock so it is released automatically when the transaction ends
    private static final String TRY_ADVISORY_XACT_LOCK_SQL = "SELECT pg_try_advisory_xact_lock(?)";

    private final DataSource dataSource;

    @Autowired
    public AdvisoryLockService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static long lockKeyFromName(String lockName) {
        return lockName.hashCode();
    }

    /**
     * Runs the given action while holding the transaction scoped advisory lock for the given key, and returns whether
     * the action was run.
     * <p>
     * A dedicated connection is taken from the pool for the lock, separate from any connection JPA uses. Work performed
     * inside the action therefore commits independently of this transaction, which is what allows failures to be
     * recorded durably even if the lock transaction is rolled back.
     *
     * @return true if the lock was acquired and the action ran, false if another instance held the lock
     */
    public boolean runIfLockAcquired(long lockKey, Runnable action) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!tryAcquireTransactionScopedLock(connection, lockKey)) {
                    return false;
                }
                action.run();
                connection.commit();
                return true;
            } catch (RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            }
        } catch (SQLException e) {
            throw new AdvisoryLockException("Failed to acquire or release advisory lock with key " + lockKey, e);
        }
    }

    private boolean tryAcquireTransactionScopedLock(Connection connection, long lockKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(TRY_ADVISORY_XACT_LOCK_SQL)) {
            statement.setLong(1, lockKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            log.error("Failed to roll back transaction holding advisory lock", rollbackException);
        }
    }

    public static class AdvisoryLockException extends RuntimeException {
        public AdvisoryLockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
