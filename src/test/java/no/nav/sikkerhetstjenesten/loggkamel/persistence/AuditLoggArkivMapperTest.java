package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.AuditLoggArkivDTO;
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
        AuditLoggArkivDTO dto = createDTO(true, false, true, true, false);
        AuditLoggArkivEntity mappedEntity = mapper.auditLoggArkivDTOToEntity(dto);

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
        assertEquals(expectedEntity.getPersonvern(), mappedEntity.getPersonvern());
        assertEquals(expectedEntity.getFiksa(), mappedEntity.getFiksa());
    }

    @Test
    void entityToDTO() {
        AuditLoggArkivEntity entity = createEntity(true, false, true);
        AuditLoggArkivDTO dto = mapper.auditLoggArkivEntityToDTO(entity);

        assertEquals(createDTO(true, false, true, true, true), dto);
    }

    private AuditLoggArkivEntity createEntity(boolean arkiv, boolean okonomi, boolean personvern) {
        return AuditLoggArkivEntity.builder()
                .id(1L)
                .created(CREATED)
                .updated(UPDATED)
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .arkiv(arkiv)
                .okonomi(okonomi)
                .personvern(personvern)
                .fiksa(FIKSA)
                .build();
    }

    private AuditLoggArkivDTO createDTO(boolean arkiv, boolean okonomi, boolean personvern, boolean loggingLeseoperasjoner, boolean loggingEndringer) {
        return AuditLoggArkivDTO.builder()
                .id(1L)
                .created(CREATED)
                .updated(UPDATED)
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .arkiv(arkiv)
                .okonomi(okonomi)
                .personvern(personvern)
                .fiksa(FIKSA)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .loggingEndringer(loggingEndringer)
                .build();
    }

}