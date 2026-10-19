package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.config.CacheConfig;
import no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig;
import no.nav.sikkerhetstjenesten.loggkamel.testsupport.DockerAvailableCondition;
import no.nav.sikkerhetstjenesten.loggkamel.testsupport.SharedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * Runs the JPA slice against the shared Postgres container with the real Flyway migrations.
 * <p>
 * Test methods run without a surrounding transaction: {@code PullRerunJPAAdapter.registerFailedPull} uses
 * REQUIRES_NEW and the advisory lock uses its own connection, so rollback-per-test would hide committed data. Tables
 * are emptied before each test instead.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
        OversiktJPAAdapter.class,
        PullRerunJPAAdapter.class,
        AuditloggTaskMapperImpl.class,
        PullRerunRequiredMapperImpl.class,
        CacheConfig.class,
        IdempotentRepositoryConfig.class
})
@ExtendWith(DockerAvailableCondition.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class PostgresRepositoryTestBase {

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgres::jdbcUrl);
        registry.add("spring.datasource.username", SharedPostgres::username);
        registry.add("spring.datasource.password", SharedPostgres::password);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    DataSource dataSource;

    JdbcTemplate jdbc;

    @SuppressWarnings("SqlWithoutWhere")
    @BeforeEach
    void cleanTables() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM pull_rerun_required");
        jdbc.update("DELETE FROM oversikt");
        jdbc.update("DELETE FROM camel_messageprocessed");
    }

    static AuditloggTaskEntity.AuditloggTaskEntityBuilder task(String dbname, TeknologiEnum teknologi) {
        return AuditloggTaskEntity.builder()
                .dbname(dbname)
                .teknologi(teknologi)
                .naisteam("testteam")
                .okonomi(false)
                .endringerUtenKrav(false)
                .loggingLeseoperasjoner(false)
                .fiksa(true)
                .funnetLogger(false)
                .discardLogs(false)
                .backfill("cold-storage-completed");
    }
}
