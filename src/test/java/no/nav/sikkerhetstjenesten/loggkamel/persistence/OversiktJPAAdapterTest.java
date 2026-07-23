package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OversiktJPAAdapterTest {

    private final static Long ID = 1L;
    private final static String DBNAME = "dbName";
    private final static TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private final static String NAISTEAM = "naisteam";
    private final static String NAISTEAM2 = "naisteam2";

    @Mock
    AuditloggTaskEntity toSaveAuditloggTaskEntity;

    @Mock
    AuditloggTaskEntity savedAuditloggTaskEntity;

    @Mock
    AuditloggTaskRequestDTO auditloggTaskRequestDTO;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO2;

    @Mock
    OversiktRepository repository;

    @Mock
    AuditloggTaskMapper mapper;

    @InjectMocks
    OversiktJPAAdapter adapter;

    @Test
    void createAuditloggTask_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(mapper.auditloggTaskRequestDTOToEntity(auditloggTaskRequestDTO)).thenReturn(toSaveAuditloggTaskEntity);
        when(repository.save(toSaveAuditloggTaskEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.createAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void createAuditloggTask_successful() {
        when(mapper.auditloggTaskRequestDTOToEntity(auditloggTaskRequestDTO)).thenReturn(toSaveAuditloggTaskEntity);
        when(repository.save(toSaveAuditloggTaskEntity)).thenReturn(savedAuditloggTaskEntity);
        when(mapper.auditloggTaskEntityToDTO(savedAuditloggTaskEntity)).thenReturn(auditloggTaskDTO);

        assertEquals(auditloggTaskDTO, adapter.createAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void updateAuditloggTask_missingEntityToUpdate() {
        when(auditloggTaskRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditloggTaskRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(null);

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void updateAuditloggTask_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(auditloggTaskRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditloggTaskRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggTaskEntity);
        when(toSaveAuditloggTaskEntity.getId()).thenReturn(ID);

        when(mapper.auditloggTaskRequestDTOToEntity(auditloggTaskRequestDTO)).thenReturn(toSaveAuditloggTaskEntity);
        when(repository.save(toSaveAuditloggTaskEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void updateAuditloggTask_successful() {
        when(auditloggTaskRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditloggTaskRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggTaskEntity);
        when(toSaveAuditloggTaskEntity.getId()).thenReturn(ID);

        when(mapper.auditloggTaskRequestDTOToEntity(auditloggTaskRequestDTO)).thenReturn(toSaveAuditloggTaskEntity);
        when(repository.save(toSaveAuditloggTaskEntity)).thenReturn(savedAuditloggTaskEntity);
        when(mapper.auditloggTaskEntityToDTO(savedAuditloggTaskEntity)).thenReturn(auditloggTaskDTO);

        assertEquals(auditloggTaskDTO, adapter.updateAuditloggTask(auditloggTaskRequestDTO));
    }

    @Test
    void findByDbnameAndTeknologi_exceptionPassesThrough() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void findByDbnameAndTeknologi_successful() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(savedAuditloggTaskEntity);
        when(mapper.auditloggTaskEntityToDTO(savedAuditloggTaskEntity)).thenReturn(auditloggTaskDTO);

        assertEquals(auditloggTaskDTO, adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void registerLogsReceivedForAuditloggTask_exceptionPassesThrough() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.registerLogsReceivedForAuditloggTask(DBNAME, TEKNOLOGI));
    }

    @Test
    void registerLogsReceivedForAuditloggTask_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggTaskEntity);
        when(repository.save(toSaveAuditloggTaskEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.registerLogsReceivedForAuditloggTask(DBNAME, TEKNOLOGI));
    }

    @Test
    void registerLogsReceivedForAuditloggTask_successful() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggTaskEntity);
        when(repository.save(toSaveAuditloggTaskEntity)).thenReturn(savedAuditloggTaskEntity);

        assertDoesNotThrow(() -> adapter.registerLogsReceivedForAuditloggTask(DBNAME, TEKNOLOGI));
        verify(toSaveAuditloggTaskEntity).setFunnetLogger(true);
    }

    @Test
    void getAllTasksByNaisteam_exceptionPassesThrough() {
        when(repository.findAllTasksByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.getAllTasksByNaisteam(NAISTEAM));
    }

    @Test
    void getAllTasksByNaisteam_successful() {
        when(repository.findAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(toSaveAuditloggTaskEntity, savedAuditloggTaskEntity));

        when(mapper.auditloggTaskEntityToDTO(toSaveAuditloggTaskEntity)).thenReturn(auditloggTaskDTO);
        when(mapper.auditloggTaskEntityToDTO(savedAuditloggTaskEntity)).thenReturn(auditloggTaskDTO2);

        assertEquals(List.of(auditloggTaskDTO, auditloggTaskDTO2), adapter.getAllTasksByNaisteam(NAISTEAM));
    }

    @Test
    void findAllDistinctNaisteam_exceptionPassesThrough() {
        when(repository.findAllDistinctNaisteam()).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.findAllDistinctNaisteam());
    }

    @Test
    void findAllDistinctNaisteam_successful() {
        when(repository.findAllDistinctNaisteam()).thenReturn(List.of(NAISTEAM, NAISTEAM2));

        assertEquals(List.of(NAISTEAM, NAISTEAM2), adapter.findAllDistinctNaisteam());
    }
}