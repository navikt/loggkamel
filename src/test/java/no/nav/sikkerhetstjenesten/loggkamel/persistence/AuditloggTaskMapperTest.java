package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditloggTaskMapperTest {

    private final static Long ID = 1L;
    private final static Instant CREATED = Instant.parse("2024-01-01T00:00:00Z");
    private final static Instant UPDATED = Instant.parse("2025-01-02T00:00:00Z");
    private final static String NAISTEAM = "testteam";
    private final static TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private final static String DBNAME = "testdb";
    private static final Boolean FIKSA = true;
    private static final Boolean FUNNET_LOGGER = true;

    AuditloggTaskMapper mapper = new AuditloggTaskMapperImpl();

    @Test
    void dtoToEntity() {
        AuditloggTaskRequestDTO dto = createRequestDTO(true, false, true);
        AuditloggTaskEntity mappedEntity = mapper.auditloggTaskRequestDTOToEntity(dto);

        AuditloggTaskEntity expectedEntity = createEntity(true, false, true);
        // null the fields that the mapper ignores
        expectedEntity.setId(null);
        expectedEntity.setCreated(null);
        expectedEntity.setUpdated(null);

        // compare fields directly because entity equals() only compares id
        assertEquals(expectedEntity.getId(), mappedEntity.getId());
        assertEquals(expectedEntity.getCreated(), mappedEntity.getCreated());
        assertEquals(expectedEntity.getUpdated(), mappedEntity.getUpdated());
        assertEquals(expectedEntity.getNaisteam(), mappedEntity.getNaisteam());
        assertEquals(expectedEntity.getTeknologi(), mappedEntity.getTeknologi());
        assertEquals(expectedEntity.getDbname(), mappedEntity.getDbname());
        assertEquals(expectedEntity.getOkonomi(), mappedEntity.getOkonomi());
        assertEquals(expectedEntity.getArkivlov(), mappedEntity.getArkivlov());
        assertEquals(expectedEntity.getLoggingLeseoperasjoner(), mappedEntity.getLoggingLeseoperasjoner());
        assertEquals(false, mappedEntity.getFiksa());
    }

    @Test
    void entityToDTO() {
        AuditloggTaskEntity entity = createEntity(true, false, true);
        AuditloggTaskDTO dto = mapper.auditloggTaskEntityToDTO(entity);

        assertEquals(createResponseDTO(true, false, true, true), dto);
    }

    private AuditloggTaskEntity createEntity(boolean arkivlov, boolean okonomi, boolean loggingLeseoperasjoner) {
        return AuditloggTaskEntity.builder()
                .id(ID)
                .created(CREATED)
                .updated(UPDATED)
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .arkivlov(arkivlov)
                .okonomi(okonomi)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .fiksa(FIKSA)
                .funnetLogger(FUNNET_LOGGER)
                .build();
    }

    private AuditloggTaskDTO createResponseDTO(boolean arkivlov, boolean okonomi, boolean loggingLeseoperasjoner, boolean loggingEndringer) {
        return AuditloggTaskDTO.builder()
                .created(CREATED)
                .updated(UPDATED)
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .arkivlov(arkivlov)
                .okonomi(okonomi)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .fiksa(FIKSA)
                .loggingEndringer(loggingEndringer)
                .funnetLogger(FUNNET_LOGGER)
                .build();
    }

    private AuditloggTaskRequestDTO createRequestDTO(boolean arkivlov, boolean okonomi, boolean loggingLeseoperasjoner) {
        return AuditloggTaskRequestDTO.builder()
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .arkivlov(arkivlov)
                .okonomi(okonomi)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .build();
    }

}