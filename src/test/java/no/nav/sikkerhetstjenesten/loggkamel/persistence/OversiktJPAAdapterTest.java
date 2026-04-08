package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;
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

    private final static Long ID = 1L;
    private final static String DBNAME = "dbName";
    private final static TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private final static String NAISTEAM = "naisteam";

    @Mock
    AuditLoggArkivEntity toSaveAuditLoggArkivEntity;

    @Mock
    AuditLoggArkivEntity savedAuditLoggArkivEntity;

    @Mock
    AuditLoggArkivRequestDTO auditLoggArkivRequestDTO;

    @Mock
    AuditLoggArkivResponseDTO auditLoggArkivResponseDTO;

    @Mock
    AuditLoggArkivResponseDTO auditLoggArkivResponseDTO2;

    @Mock
    OversiktRepository repository;

    @Mock
    AuditLoggArkivMapper mapper;

    @InjectMocks
    OversiktJPAAdapter adapter;

    @Test
    void createAuditLoggArkiv_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(mapper.auditLoggArkivRequestDTOToEntity(auditLoggArkivRequestDTO)).thenReturn(toSaveAuditLoggArkivEntity);
        when(repository.save(toSaveAuditLoggArkivEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.createAuditLoggArkiv(auditLoggArkivRequestDTO));
    }

    @Test
    void createAuditLoggArkiv_successful() {
        when(mapper.auditLoggArkivRequestDTOToEntity(auditLoggArkivRequestDTO)).thenReturn(toSaveAuditLoggArkivEntity);
        when(repository.save(toSaveAuditLoggArkivEntity)).thenReturn(savedAuditLoggArkivEntity);
        when(mapper.auditLoggArkivEntityToResponseDTO(savedAuditLoggArkivEntity)).thenReturn(auditLoggArkivResponseDTO);

        assertEquals(auditLoggArkivResponseDTO, adapter.createAuditLoggArkiv(auditLoggArkivRequestDTO));
    }

    @Test
    void updateAuditLoggArkiv_missingEntityToUpdate() {
        when(auditLoggArkivRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditLoggArkivRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(null);

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateAuditLoggArkiv(auditLoggArkivRequestDTO));
    }

    @Test
    void updateAuditLoggArkiv_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(auditLoggArkivRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditLoggArkivRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditLoggArkivEntity);
        when(toSaveAuditLoggArkivEntity.getId()).thenReturn(ID);

        when(mapper.auditLoggArkivRequestDTOToEntity(auditLoggArkivRequestDTO)).thenReturn(toSaveAuditLoggArkivEntity);
        when(repository.save(toSaveAuditLoggArkivEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateAuditLoggArkiv(auditLoggArkivRequestDTO));
    }

    @Test
    void updateAuditLoggArkiv_successful() {
        when(auditLoggArkivRequestDTO.getDbname()).thenReturn(DBNAME);
        when(auditLoggArkivRequestDTO.getTeknologi()).thenReturn(TEKNOLOGI);
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(toSaveAuditLoggArkivEntity);
        when(toSaveAuditLoggArkivEntity.getId()).thenReturn(ID);

        when(mapper.auditLoggArkivRequestDTOToEntity(auditLoggArkivRequestDTO)).thenReturn(toSaveAuditLoggArkivEntity);
        when(repository.save(toSaveAuditLoggArkivEntity)).thenReturn(savedAuditLoggArkivEntity);
        when(mapper.auditLoggArkivEntityToResponseDTO(savedAuditLoggArkivEntity)).thenReturn(auditLoggArkivResponseDTO);

        assertEquals(auditLoggArkivResponseDTO, adapter.updateAuditLoggArkiv(auditLoggArkivRequestDTO));
    }

    @Test
    void findByDbnameAndTeknologi_exceptionPassesThrough() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void findByDbnameAndTeknologi_successful() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(savedAuditLoggArkivEntity);
        when(mapper.auditLoggArkivEntityToResponseDTO(savedAuditLoggArkivEntity)).thenReturn(auditLoggArkivResponseDTO);

        assertEquals(auditLoggArkivResponseDTO, adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAllTasksByNaisteam_exceptionPassesThrough() {
        when(repository.findAllByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.getAllTasksByNaisteam(NAISTEAM));
    }

    @Test
    void getAllTasksByNaisteam_successful() {
        when(repository.findAllByNaisteam(NAISTEAM)).thenReturn(List.of(toSaveAuditLoggArkivEntity, savedAuditLoggArkivEntity));

        when(mapper.auditLoggArkivEntityToResponseDTO(toSaveAuditLoggArkivEntity)).thenReturn(auditLoggArkivResponseDTO);
        when(mapper.auditLoggArkivEntityToResponseDTO(savedAuditLoggArkivEntity)).thenReturn(auditLoggArkivResponseDTO2);

        assertEquals(List.of(auditLoggArkivResponseDTO, auditLoggArkivResponseDTO2), adapter.getAllTasksByNaisteam(NAISTEAM));
    }
}