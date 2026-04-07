package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.rest.BackupTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.OversiktJPAAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OversiktServiceTest {

    private static final String DBNAME = "dbName";
    private static final TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private static final String NAISTEAM = "naisteam";

    @Mock
    BackupTaskDTO requestBackupTaskDTO;

    @Mock
    BackupTaskDTO responseBackupTaskDTO;

    @Mock
    OversiktJPAAdapter adapter;

    @InjectMocks
    OversiktService service;

    @Test
    void createBackupTask_successful() {
        when(adapter.createBackupTask(requestBackupTaskDTO)).thenReturn(responseBackupTaskDTO);

        assertEquals(responseBackupTaskDTO, service.createBackupTask(requestBackupTaskDTO));
    }

    @Test
    void createBackupTask_exceptionPassesThrough() {
        when(adapter.createBackupTask(requestBackupTaskDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.createBackupTask(requestBackupTaskDTO));
    }

    @Test
    void updateBackupTask_successful() {
        when(adapter.updateBackupTask(requestBackupTaskDTO)).thenReturn(responseBackupTaskDTO);

        assertEquals(responseBackupTaskDTO, service.updateBackupTask(requestBackupTaskDTO));
    }

    @Test
    void updateBackupTask_exceptionPassesThrough() {
        when(adapter.updateBackupTask(requestBackupTaskDTO)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.updateBackupTask(requestBackupTaskDTO));
    }

    @Test
    void getBackupTaskByDbnameAndTeknologi_successful() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(responseBackupTaskDTO);

        assertEquals(responseBackupTaskDTO, service.getBackupTaskByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getBackupTaskByDbnameAndTeknologi_exceptionPassesThrough() {
        when(adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getBackupTaskByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getBackupTaskByNaisteam_successful() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(requestBackupTaskDTO, responseBackupTaskDTO));

        assertEquals(List.of(requestBackupTaskDTO, responseBackupTaskDTO), service.getBackupTaskByNaisteam(NAISTEAM));
    }

    @Test
    void getBackupTaskByNaisteam_exceptionPassesThrough() {
        when(adapter.getAllTasksByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> service.getBackupTaskByNaisteam(NAISTEAM));
    }
}