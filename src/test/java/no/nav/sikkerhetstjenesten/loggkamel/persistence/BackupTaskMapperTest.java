package no.nav.sikkerhetstjenesten.loggkamel.persistence;

import no.nav.sikkerhetstjenesten.loggkamel.rest.BackupTaskDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BackupTaskMapperTest {

    private final static Long ID = 1L;
    private final static Instant CREATED = Instant.parse("2024-01-01T00:00:00Z");
    private final static Instant UPDATED = Instant.parse("2025-01-02T00:00:00Z");
    private final static String NAISTEAM = "testteam";
    private final static TeknologiEnum TEKNOLOGI = TeknologiEnum.ORACLE;
    private final static String DBNAME = "testdb";
    private static final Boolean FIKSA = true;

    BackupTaskMapper mapper = new BackupTaskMapperImpl();

    @Test
    void dtoToEntity() {
        BackupTaskDTO dto = createDTO(true, false, true, true, false);
        BackupTaskEntity entity = mapper.backupTaskDTOToEntity(dto);

        assertEquals(createEntity(true, false, true), entity);
    }

    @Test
    void entityToDTO() {
        BackupTaskEntity entity = createEntity(true, false, true);
        BackupTaskDTO dto = mapper.backupTaskEntityToDTO(entity);

        assertEquals(createDTO(true, false, true, true, true), dto);
    }

    private BackupTaskEntity createEntity(boolean arkiv, boolean okonomi, boolean personvern) {
        return BackupTaskEntity.builder()
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

    private BackupTaskDTO createDTO(boolean arkiv, boolean okonomi, boolean personvern, boolean loggingLeseoperasjoner, boolean loggingEndringer) {
        return BackupTaskDTO.builder()
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