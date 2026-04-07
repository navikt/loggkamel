package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.AuditLoggArkivDTO;
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

    private final static String DBNAME = "dbName";
    private final static TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private final static String NAISTEAM = "naisteam";

    @Mock
    AuditLoggArkivEntity toSaveAuditLoggArkivEntity;

    @Mock
    AuditLoggArkivEntity savedAuditLoggArkivEntity;

    @Mock
    AuditLoggArkivDTO toSaveAuditLoggArkivDTO;

    @Mock
    AuditLoggArkivDTO savedAuditLoggArkivDTO;

    @Mock
    OversiktRepository repository;

    @Mock
    AuditLoggArkivMapper mapper;

    @InjectMocks
    OversiktJPAAdapter adapter;

    @Test
    void createAuditLoggArkiv_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(mapper.auditLoggArkivDTOToEntity(toSaveAuditLoggArkivDTO)).thenReturn(toSaveAuditLoggArkivEntity);
        when(repository.save(toSaveAuditLoggArkivEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.createAuditLoggArkiv(toSaveAuditLoggArkivDTO));
    }

    @Test
    void createAuditLoggArkiv_successful() {
        when(mapper.auditLoggArkivDTOToEntity(toSaveAuditLoggArkivDTO)).thenReturn(toSaveAuditLoggArkivEntity);
        when(repository.save(toSaveAuditLoggArkivEntity)).thenReturn(savedAuditLoggArkivEntity);
        when(mapper.auditLoggArkivEntityToDTO(savedAuditLoggArkivEntity)).thenReturn(savedAuditLoggArkivDTO);

        assertEquals(savedAuditLoggArkivDTO, adapter.createAuditLoggArkiv(toSaveAuditLoggArkivDTO));
    }

    @Test
    void updateAuditLoggArkiv_missingEntityToUpdate() {
        when(toSaveAuditLoggArkivDTO.getId()).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(java.util.Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateAuditLoggArkiv(toSaveAuditLoggArkivDTO));
    }

    @Test
    void updateAuditLoggArkiv_forbiddenOperationExceptionOnDataIntegrityViolation() {
        when(toSaveAuditLoggArkivDTO.getId()).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(toSaveAuditLoggArkivEntity));

        when(mapper.auditLoggArkivDTOToEntity(toSaveAuditLoggArkivDTO)).thenReturn(toSaveAuditLoggArkivEntity);
        when(repository.save(toSaveAuditLoggArkivEntity)).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ForbiddenOperationException.class, () -> adapter.updateAuditLoggArkiv(toSaveAuditLoggArkivDTO));
    }

    @Test
    void updateAuditLoggArkiv_successful() {
        when(toSaveAuditLoggArkivDTO.getId()).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(toSaveAuditLoggArkivEntity));

        when(mapper.auditLoggArkivDTOToEntity(toSaveAuditLoggArkivDTO)).thenReturn(toSaveAuditLoggArkivEntity);
        when(repository.save(toSaveAuditLoggArkivEntity)).thenReturn(savedAuditLoggArkivEntity);
        when(mapper.auditLoggArkivEntityToDTO(savedAuditLoggArkivEntity)).thenReturn(savedAuditLoggArkivDTO);

        assertEquals(savedAuditLoggArkivDTO, adapter.updateAuditLoggArkiv(toSaveAuditLoggArkivDTO));
    }

    @Test
    void findByDbnameAndTeknologi_exceptionPassesThrough() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void findByDbnameAndTeknologi_successful() {
        when(repository.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI)).thenReturn(savedAuditLoggArkivEntity);
        when(mapper.auditLoggArkivEntityToDTO(savedAuditLoggArkivEntity)).thenReturn(savedAuditLoggArkivDTO);

        assertEquals(savedAuditLoggArkivDTO, adapter.findByDbnameAndTeknologi(DBNAME, TEKNOLOGI));
    }

    @Test
    void getAllTasksByNaisteam_exceptionPassesThrough() {
        when(repository.findAllByNaisteam(NAISTEAM)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> adapter.getAllTasksByNaisteam(NAISTEAM));
    }

    @Test
    void getAllTasksByNaisteam_successful() {
        when(repository.findAllByNaisteam(NAISTEAM)).thenReturn(List.of(toSaveAuditLoggArkivEntity, savedAuditLoggArkivEntity));

        when(mapper.auditLoggArkivEntityToDTO(toSaveAuditLoggArkivEntity)).thenReturn(toSaveAuditLoggArkivDTO);
        when(mapper.auditLoggArkivEntityToDTO(savedAuditLoggArkivEntity)).thenReturn(savedAuditLoggArkivDTO);

        assertEquals(List.of(toSaveAuditLoggArkivDTO, savedAuditLoggArkivDTO), adapter.getAllTasksByNaisteam(NAISTEAM));
    }
}