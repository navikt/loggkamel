package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskRequestDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;

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
    private static final Boolean DISCARD_LOGS = true;
    private static final BackfillStatus BACKFILL = BackfillStatus.BACKFILL_REQUESTED;

    AuditloggTaskMapper mapper = new AuditloggTaskMapperImpl();

    @Test
    void dtoToEntity() {
        for (AuditloggTaskEntity defaults : List.of(new AuditloggTaskEntity(), AuditloggTaskEntity.builder().build())) {
            assertEquals(false, defaults.getOkonomi());
            assertEquals(false, defaults.getArkivforskrift());
            assertEquals(false, defaults.getEndringerUtenKrav());
            assertEquals(false, defaults.getLoggingLeseoperasjoner());
            assertEquals(false, defaults.getFiksa());
            assertEquals(false, defaults.getFunnetLogger());
            assertEquals(false, defaults.getDiscardLogs());
            assertEquals("backfill-completed", defaults.getBackfill());
            assertNull(defaults.getId());
            assertNull(defaults.getCreated());
            assertNull(defaults.getUpdated());
        }

        AuditloggTaskRequestDTO dto = createRequestDTO(false, false, true, true);
        AuditloggTaskEntity mappedEntity = mapper.auditloggTaskRequestDTOToEntity(dto);

        AuditloggTaskEntity expectedEntity = createEntity(false, false, true);
        expectedEntity.setArkivforskrift(true);
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
        assertEquals(expectedEntity.getArkivforskrift(), mappedEntity.getArkivforskrift());
        assertEquals(expectedEntity.getEndringerUtenKrav(), mappedEntity.getEndringerUtenKrav());
        assertEquals(expectedEntity.getLoggingLeseoperasjoner(), mappedEntity.getLoggingLeseoperasjoner());
        assertEquals(expectedEntity.getBackfill(), mappedEntity.getBackfill());
        assertEquals(false, mappedEntity.getFiksa());
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, false, false",
            "true, false, false, true",
            "false, true, false, true",
            "false, false, true, true",
            "false, true, true, true"
    })
    void entityToDTO(boolean endringerUtenKrav, boolean okonomi, boolean arkivforskrift, boolean loggingEndringer) {
        AuditloggTaskEntity entity = createEntity(endringerUtenKrav, okonomi, true);
        entity.setArkivforskrift(arkivforskrift);
        AuditloggTaskDTO dto = mapper.auditloggTaskEntityToDTO(entity);

        assertEquals(createResponseDTO(endringerUtenKrav, okonomi, arkivforskrift, true, loggingEndringer), dto);
    }

    @ParameterizedTest
    @EnumSource(BackfillStatus.class)
    void backfillStatusRoundTripsThroughEntityAndDTO(BackfillStatus status) {
        AuditloggTaskRequestDTO requestDTO = AuditloggTaskRequestDTO.builder()
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .endringerUtenKrav(true)
                .okonomi(false)
                .loggingLeseoperasjoner(true)
                .backfill(status)
                .build();

        AuditloggTaskEntity entity = mapper.auditloggTaskRequestDTOToEntity(requestDTO);
        assertEquals(status.getValue(), entity.getBackfill());
        assertFalse(entity.getArkivforskrift());

        AuditloggTaskDTO dto = mapper.auditloggTaskEntityToDTO(entity);
        assertEquals(status, dto.getBackfill());
    }

    private AuditloggTaskEntity createEntity(boolean endringerUtenKrav, boolean okonomi, boolean loggingLeseoperasjoner) {
        return AuditloggTaskEntity.builder()
                .id(ID)
                .created(CREATED)
                .updated(UPDATED)
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .endringerUtenKrav(endringerUtenKrav)
                .okonomi(okonomi)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .fiksa(FIKSA)
                .funnetLogger(FUNNET_LOGGER)
                .discardLogs(DISCARD_LOGS)
                .backfill(BACKFILL.getValue())
                .build();
    }

    private AuditloggTaskDTO createResponseDTO(boolean endringerUtenKrav, boolean okonomi, boolean arkivforskrift, boolean loggingLeseoperasjoner, boolean loggingEndringer) {
        return AuditloggTaskDTO.builder()
                .created(CREATED)
                .updated(UPDATED)
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .endringerUtenKrav(endringerUtenKrav)
                .okonomi(okonomi)
                .arkivforskrift(arkivforskrift)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .fiksa(FIKSA)
                .loggingEndringer(loggingEndringer)
                .funnetLogger(FUNNET_LOGGER)
                .discardLogs(DISCARD_LOGS)
                .backfill(BACKFILL)
                .build();
    }

    private AuditloggTaskRequestDTO createRequestDTO(boolean endringerUtenKrav, boolean okonomi, boolean arkivforskrift, boolean loggingLeseoperasjoner) {
        return AuditloggTaskRequestDTO.builder()
                .naisteam(NAISTEAM)
                .teknologi(TEKNOLOGI)
                .dbname(DBNAME)
                .endringerUtenKrav(endringerUtenKrav)
                .okonomi(okonomi)
                .arkivforskrift(arkivforskrift)
                .loggingLeseoperasjoner(loggingLeseoperasjoner)
                .backfill(BACKFILL)
                .build();
    }

}