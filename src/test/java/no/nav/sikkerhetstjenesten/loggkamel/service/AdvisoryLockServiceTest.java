package no.nav.sikkerhetstjenesten.loggkamel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvisoryLockServiceTest {

    private static final long LOCK_KEY = 42L;

    @Mock
    DataSource dataSource;

    @Mock
    Connection connection;

    @Mock
    PreparedStatement preparedStatement;

    @Mock
    ResultSet resultSet;

    @InjectMocks
    AdvisoryLockService advisoryLockService;

    @BeforeEach
    void setUpConnection() throws SQLException {
        org.mockito.Mockito.lenient().when(dataSource.getConnection()).thenReturn(connection);
    }

    private void stubLockAcquisition(boolean lockGranted) throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(lockGranted);
    }

    @Test
    void lockKeyFromName_isStableAndDistinguishesNames() {
        assertEquals(AdvisoryLockService.lockKeyFromName("loggkamel.pull.db2.scheduled"),
                AdvisoryLockService.lockKeyFromName("loggkamel.pull.db2.scheduled"));
        assertNotEquals(AdvisoryLockService.lockKeyFromName("loggkamel.pull.db2.scheduled"),
                AdvisoryLockService.lockKeyFromName("loggkamel.pull.oracle.scheduled"));
    }

    @Test
    void runIfLockAcquired_runsActionAndCommitsWhenLockIsGranted() throws SQLException {
        stubLockAcquisition(true);
        AtomicBoolean actionRan = new AtomicBoolean(false);

        boolean lockAcquired = advisoryLockService.runIfLockAcquired(LOCK_KEY, () -> actionRan.set(true));

        assertTrue(lockAcquired);
        assertTrue(actionRan.get());
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).close();
    }

    @Test
    void runIfLockAcquired_doesNotRunActionWhenLockIsHeldElsewhere() throws SQLException {
        stubLockAcquisition(false);
        AtomicBoolean actionRan = new AtomicBoolean(false);

        boolean lockAcquired = advisoryLockService.runIfLockAcquired(LOCK_KEY, () -> actionRan.set(true));

        assertFalse(lockAcquired);
        assertFalse(actionRan.get());
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    void runIfLockAcquired_rollsBackAndRethrowsWhenActionFails() throws SQLException {
        stubLockAcquisition(true);

        RuntimeException actionFailure = new RuntimeException("pull failed");

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> advisoryLockService.runIfLockAcquired(LOCK_KEY, () -> {
                    throw actionFailure;
                }));

        assertSame(thrown, actionFailure);
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
    }

    @Test
    void runIfLockAcquired_wrapsSqlFailures() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("connection lost"));

        assertThrows(AdvisoryLockService.AdvisoryLockException.class,
                () -> advisoryLockService.runIfLockAcquired(LOCK_KEY, () -> {
                }));

        verify(connection).close();
    }
}
