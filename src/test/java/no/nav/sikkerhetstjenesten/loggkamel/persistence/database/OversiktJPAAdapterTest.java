package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.SQLException;
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

    @ParameterizedTest
    @CsvSource({
            "23505, oversikt_dbname_teknologi_unique, true",
            "23505, other_unique_constraint, false",
            "23514, oversikt_endringer_uten_krav_valid, false",
            "23514, oversikt_dbname_teknologi_unique, false",
            "23502, arkivforskrift, false"
    })
    void createAuditloggTask_translatesOnlyTaskIdentityDuplicates(String sqlState, String constraint, boolean duplicate) {
        when(mapper.auditloggTaskRequestDTOToEntity(auditloggTaskRequestDTO)).thenReturn(toSaveAuditloggTaskEntity);
        var exception = new DataIntegrityViolationException("Integrity violation",
                new ConstraintViolationException("Constraint violation", new SQLException("Rejected", sqlState), constraint));
        when(repository.save(toSaveAuditloggTaskEntity)).thenThrow(exception);

        if (duplicate) {
            assertThrows(ForbiddenOperationException.class, () -> adapter.createAuditloggTask(auditloggTaskRequestDTO));
        } else {
            assertSame(exception, assertThrows(DataIntegrityViolationException.class,
                    () -> adapter.createAuditloggTask(auditloggTaskRequestDTO)));
        }
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
    void updateAuditloggTask_preservesUnclassifiedDataIntegrityViolation() {
        when(auditloggTaskRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditloggTaskRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggTaskEntity);
        when(toSaveAuditloggTaskEntity.getId()).thenReturn(ID);

        when(mapper.auditloggTaskRequestDTOToEntity(auditloggTaskRequestDTO)).thenReturn(toSaveAuditloggTaskEntity);
        var exception = new DataIntegrityViolationException("Integrity violation");
        when(repository.save(toSaveAuditloggTaskEntity)).thenThrow(exception);

        assertSame(exception, assertThrows(DataIntegrityViolationException.class,
                () -> adapter.updateAuditloggTask(auditloggTaskRequestDTO)));
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
    void registerLogsReceivedForAuditloggTask_preservesUnclassifiedDataIntegrityViolation() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggTaskEntity);
        when(repository.save(toSaveAuditloggTaskEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(DataIntegrityViolationException.class, () -> adapter.registerLogsReceivedForAuditloggTask(DBNAME, TEKNOLOGI));
    }

    @Test
    void registerLogsReceivedForAuditloggTask_successful() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggTaskEntity);
        when(repository.save(toSaveAuditloggTaskEntity)).thenReturn(savedAuditloggTaskEntity);

        assertDoesNotThrow(() -> adapter.registerLogsReceivedForAuditloggTask(DBNAME, TEKNOLOGI));
        verify(toSaveAuditloggTaskEntity).setFunnetLogger(true);
    }

    @Test
    void getTasksRegisteredToNaisteam_exceptionPassesThrough() {
        when(repository.findAllTasksByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.getTasksRegisteredToNaisteam(NAISTEAM));
    }

    @Test
    void getTasksRegisteredToNaisteam_successful() {
        when(repository.findAllTasksByNaisteam(NAISTEAM)).thenReturn(List.of(toSaveAuditloggTaskEntity, savedAuditloggTaskEntity));

        when(mapper.auditloggTaskEntityToDTO(toSaveAuditloggTaskEntity)).thenReturn(auditloggTaskDTO);
        when(mapper.auditloggTaskEntityToDTO(savedAuditloggTaskEntity)).thenReturn(auditloggTaskDTO2);

        assertEquals(List.of(auditloggTaskDTO, auditloggTaskDTO2), adapter.getTasksRegisteredToNaisteam(NAISTEAM));
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

    @Test
    void findConfiguredTasksByTeknologiAndBackfill_mapsMatchingEntities() {
        when(repository.findAllByTeknologiAndFiksaTrueAndBackfill(TEKNOLOGI, BackfillStatus.BACKFILL_REQUESTED.getValue()))
                .thenReturn(List.of(toSaveAuditloggTaskEntity));
        when(mapper.auditloggTaskEntityToDTO(toSaveAuditloggTaskEntity)).thenReturn(auditloggTaskDTO);

        assertEquals(
                List.of(auditloggTaskDTO),
                adapter.findConfiguredTasksByTeknologiAndBackfill(TEKNOLOGI, BackfillStatus.BACKFILL_REQUESTED));
    }

}