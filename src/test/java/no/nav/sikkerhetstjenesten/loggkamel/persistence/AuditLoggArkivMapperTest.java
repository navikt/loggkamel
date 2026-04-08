package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditLoggArkivResponseDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditLoggArkivMapperTest {

    private final static Long ID = 1L;
    private final static Instant CREATED = Instant.parse("2024-01-01T00:00:00Z");
    private final static Instant UPDATED = Instant.parse("2025-01-02T00:00:00Z");
    private final static String NAISTEAM = "testteam";
    private final static TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private final static String DBNAME = "testdb";
    private static final Boolean FIKSA = true;

    AuditLoggArkivMapper mapper = new AuditLoggArkivMapperImpl();

    @Test
    void dtoToEntity() {
        AuditLoggArkivRequestDTO dto = createRequestDTO(true, false, true);
        AuditLoggArkivEntity mappedEntity = mapper.auditLoggArkivRequestDTOToEntity(dto);

        AuditLoggArkivEntity expectedEntity = createEntity(true, false, true);
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
        assertEquals(expectedEntity.getArkiv(), mappedEntity.getArkiv());
        assertEquals(expectedEntity.getLoggingLeseoperasjoner(), mappedEntity.getLoggingLeseoperasjoner());
        assertEquals(expectedEntity.getFiksa(), mappedEntity.getFiksa());
    }

    @Test
    void entityToDTO() {
        AuditLoggArkivEntity entity = createEntity(true, false, true);
        AuditLoggArkivResponseDTO dto = mapper.auditLoggArkivEntityToResponseDTO(entity);

        assertEquals(createResponseDTO(true, false, true, true), dto);
    }

    private AuditLoggArkivEntity createEntity(boolean arkiv, boolean okonomi, boolean loggingLeseoperasjoner) {
        return AuditLoggArkivEntity.builder()
                .id(ID)
                .created(CREATED)
                .updated(UPDATED)
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .arkiv(arkiv)
                .okonomi(okonomi)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .fiksa(FIKSA)
                .build();
    }

    private AuditLoggArkivResponseDTO createResponseDTO(boolean arkiv, boolean okonomi, boolean loggingLeseoperasjoner, boolean loggingEndringer) {
        return AuditLoggArkivResponseDTO.builder()
                .created(CREATED)
                .updated(UPDATED)
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .arkiv(arkiv)
                .okonomi(okonomi)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .fiksa(FIKSA)
                .loggingEndringer(loggingEndringer)
                .build();
    }

    private AuditLoggArkivRequestDTO createRequestDTO(boolean arkiv, boolean okonomi, boolean loggingLeseoperasjoner) {
        return AuditLoggArkivRequestDTO.builder()
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .arkiv(arkiv)
                .okonomi(okonomi)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .fiksa(FIKSA)
                .build();
    }

}