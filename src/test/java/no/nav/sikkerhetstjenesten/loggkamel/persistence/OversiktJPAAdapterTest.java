package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
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

    @Mock
    AuditloggArkivEntity toSaveAuditloggArkivEntity;

    @Mock
    AuditloggArkivEntity savedAuditloggArkivEntity;

    @Mock
    AuditloggArkivRequestDTO auditloggArkivRequestDTO;

    @Mock
    AuditloggArkivResponseDTO auditloggArkivResponseDTO;

    @Mock
    AuditloggArkivResponseDTO auditloggArkivResponseDTO2;

    @Mock
    OversiktRepository repository;

    @Mock
    AuditloggArkivMapper mapper;

    @InjectMocks
    OversiktJPAAdapter adapter;

    @Test
    void createAuditloggArkiv_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(mapper.auditloggArkivRequestDTOToEntity(auditloggArkivRequestDTO)).thenReturn(toSaveAuditloggArkivEntity);
        when(repository.save(toSaveAuditloggArkivEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.createAuditloggArkiv(auditloggArkivRequestDTO));
    }

    @Test
    void createAuditloggArkiv_successful() {
        when(mapper.auditloggArkivRequestDTOToEntity(auditloggArkivRequestDTO)).thenReturn(toSaveAuditloggArkivEntity);
        when(repository.save(toSaveAuditloggArkivEntity)).thenReturn(savedAuditloggArkivEntity);
        when(mapper.auditloggArkivEntityToResponseDTO(savedAuditloggArkivEntity)).thenReturn(auditloggArkivResponseDTO);

        assertEquals(auditloggArkivResponseDTO, adapter.createAuditloggArkiv(auditloggArkivRequestDTO));
    }

    @Test
    void updateAuditloggArkiv_missingEntityToUpdate() {
        when(auditloggArkivRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditloggArkivRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(null);

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateAuditloggArkiv(auditloggArkivRequestDTO));
    }

    @Test
    void updateAuditloggArkiv_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(auditloggArkivRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditloggArkivRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggArkivEntity);
        when(toSaveAuditloggArkivEntity.getId()).thenReturn(ID);

        when(mapper.auditloggArkivRequestDTOToEntity(auditloggArkivRequestDTO)).thenReturn(toSaveAuditloggArkivEntity);
        when(repository.save(toSaveAuditloggArkivEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateAuditloggArkiv(auditloggArkivRequestDTO));
    }

    @Test
    void updateAuditloggArkiv_successful() {
        when(auditloggArkivRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditloggArkivRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggArkivEntity);
        when(toSaveAuditloggArkivEntity.getId()).thenReturn(ID);

        when(mapper.auditloggArkivRequestDTOToEntity(auditloggArkivRequestDTO)).thenReturn(toSaveAuditloggArkivEntity);
        when(repository.save(toSaveAuditloggArkivEntity)).thenReturn(savedAuditloggArkivEntity);
        when(mapper.auditloggArkivEntityToResponseDTO(savedAuditloggArkivEntity)).thenReturn(auditloggArkivResponseDTO);

        assertEquals(auditloggArkivResponseDTO, adapter.updateAuditloggArkiv(auditloggArkivRequestDTO));
    }

    @Test
    void findByDbnameAndTeknologi_exceptionPassesThrough() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void findByDbnameAndTeknologi_successful() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(savedAuditloggArkivEntity);
        when(mapper.auditloggArkivEntityToResponseDTO(savedAuditloggArkivEntity)).thenReturn(auditloggArkivResponseDTO);

        assertEquals(auditloggArkivResponseDTO, adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void registerLogsReceivedForAuditloggArkiv_exceptionPassesThrough() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.registerLogsReceivedForAuditloggArkiv(DBNAME, TEKNOLOGI));
    }

    @Test
    void registerLogsReceivedForAuditloggArkiv_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggArkivEntity);
        when(repository.save(toSaveAuditloggArkivEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.registerLogsReceivedForAuditloggArkiv(DBNAME, TEKNOLOGI));
    }

    @Test
    void registerLogsReceivedForAuditloggArkiv_successful() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditloggArkivEntity);
        when(repository.save(toSaveAuditloggArkivEntity)).thenReturn(savedAuditloggArkivEntity);

        assertDoesNotThrow(() -> adapter.registerLogsReceivedForAuditloggArkiv(DBNAME, TEKNOLOGI));
        verify(toSaveAuditloggArkivEntity).setFunnetLogger(true);
    }

    @Test
    void getAllTasksByNaisteam_exceptionPassesThrough() {
        when(repository.findAllByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.getAllTasksByNaisteam(NAISTEAM));
    }

    @Test
    void getAllTasksByNaisteam_successful() {
        when(repository.findAllByNaisteam(NAISTEAM)).thenReturn(List.of(toSaveAuditloggArkivEntity, savedAuditloggArkivEntity));

        when(mapper.auditloggArkivEntityToResponseDTO(toSaveAuditloggArkivEntity)).thenReturn(auditloggArkivResponseDTO);
        when(mapper.auditloggArkivEntityToResponseDTO(savedAuditloggArkivEntity)).thenReturn(auditloggArkivResponseDTO2);

        assertEquals(List.of(auditloggArkivResponseDTO, auditloggArkivResponseDTO2), adapter.getAllTasksByNaisteam(NAISTEAM));
    }
}