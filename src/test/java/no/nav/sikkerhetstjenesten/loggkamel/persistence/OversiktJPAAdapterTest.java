package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.controller.BackupTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.controller.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OversiktJPAAdapterTest {

    private final static String DBNAME = "dbName";
    private final static TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private final static String NAISTEAM = "naisteam";

    @Mock
    BackupTaskEntity toSaveBackupTaskEntity;

    @Mock
    BackupTaskEntity savedBackupTaskEntity;

    @Mock
    BackupTaskDTO toSaveBackupTaskDTO;

    @Mock
    BackupTaskDTO savedBackupTaskDTO;

    @Mock
    OversiktRepository repository;

    @Mock
    BackupTaskMapper mapper;

    @InjectMocks
    OversiktJPAAdapter adapter;

    @Test
    void createBackupTask_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(mapper.backupTaskDTOToEntity(toSaveBackupTaskDTO)).thenReturn(toSaveBackupTaskEntity);
        when(repository.save(toSaveBackupTaskEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.createBackupTask(toSaveBackupTaskDTO));
    }

    @Test
    void createBackupTask_successful() {
        when(mapper.backupTaskDTOToEntity(toSaveBackupTaskDTO)).thenReturn(toSaveBackupTaskEntity);
        when(repository.save(toSaveBackupTaskEntity)).thenReturn(savedBackupTaskEntity);
        when(mapper.backupTaskEntityToDTO(savedBackupTaskEntity)).thenReturn(savedBackupTaskDTO);

        assertEquals(savedBackupTaskDTO, adapter.createBackupTask(toSaveBackupTaskDTO));
    }

    @Test
    void updateBackupTask_missingEntityToUpdate() {
        when(toSaveBackupTaskDTO.getId()).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(java.util.Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateBackupTask(toSaveBackupTaskDTO));
    }

    @Test
    void updateBackupTask_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(toSaveBackupTaskDTO.getId()).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(toSaveBackupTaskEntity));

        when(mapper.backupTaskDTOToEntity(toSaveBackupTaskDTO)).thenReturn(toSaveBackupTaskEntity);
        when(repository.save(toSaveBackupTaskEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateBackupTask(toSaveBackupTaskDTO));
    }

    @Test
    void updateBackupTask_successful() {
        when(toSaveBackupTaskDTO.getId()).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(toSaveBackupTaskEntity));

        when(mapper.backupTaskDTOToEntity(toSaveBackupTaskDTO)).thenReturn(toSaveBackupTaskEntity);
        when(repository.save(toSaveBackupTaskEntity)).thenReturn(savedBackupTaskEntity);
        when(mapper.backupTaskEntityToDTO(savedBackupTaskEntity)).thenReturn(savedBackupTaskDTO);

        assertEquals(savedBackupTaskDTO, adapter.updateBackupTask(toSaveBackupTaskDTO));
    }

    @Test
    void findByDbnameAndTeknologi_exceptionPassesThrough() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void findByDbnameAndTeknologi_successful() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(savedBackupTaskEntity);
        when(mapper.backupTaskEntityToDTO(savedBackupTaskEntity)).thenReturn(savedBackupTaskDTO);

        assertEquals(savedBackupTaskDTO, adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAllTasksByNaisteam_exceptionPassesThrough() {
        when(repository.findAllByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.getAllTasksByNaisteam(NAISTEAM));
    }

    @Test
    void getAllTasksByNaisteam_successful() {
        when(repository.findAllByNaisteam(NAISTEAM)).thenReturn(List.of(toSaveBackupTaskEntity, savedBackupTaskEntity));

        when(mapper.backupTaskEntityToDTO(toSaveBackupTaskEntity)).thenReturn(toSaveBackupTaskDTO);
        when(mapper.backupTaskEntityToDTO(savedBackupTaskEntity)).thenReturn(savedBackupTaskDTO);

        assertEquals(List.of(toSaveBackupTaskDTO, savedBackupTaskDTO), adapter.getAllTasksByNaisteam(NAISTEAM));
    }
}