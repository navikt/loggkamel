package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import no.nav.sikkerhetstjenesten.loggkamel.testsupport.DockerAvailableCondition;
import no.nav.sikkerhetstjenesten.loggkamel.testsupport.SharedPostgres;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Applies every migration to an empty throwaway database in the shared container. */
@ExtendWith(DockerAvailableCondition.class)
class FlywayMigrationTest {

    private static final String DATABASE = "flyway_migration_test";
    private static String url;

    @BeforeAll
    static void createDatabase() {
        url = SharedPostgres.createDatabase(DATABASE);
    }

    @AfterAll
    static void dropDatabase() {
        SharedPostgres.dropDatabase(DATABASE);
    }

    @Test
    void allMigrationsApplyToEmptyDatabaseAndSecondRunIsNoOp() throws IOException {
        Flyway flyway = Flyway.configure()
                .dataSource(url, SharedPostgres.username(), SharedPostgres.password())
                .locations("classpath:db/migration")
                .load();

        MigrateResult first = flyway.migrate();

        int migrationFiles = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/V*__*.sql").length;
        assertThat(first.success).isTrue();
        assertThat(first.migrationsExecuted).isEqualTo(migrationFiles);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
    }

    @Test
    void migratedSchemaContainsTheApplicationTables() {
        Flyway.configure()
                .dataSource(url, SharedPostgres.username(), SharedPostgres.password())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, SharedPostgres.username(), SharedPostgres.password()));

        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables).contains("oversikt", "pull_rerun_required");
    }
}
