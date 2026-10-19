package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate checks the mapped entities against the schema produced by the Flyway migrations. A mismatch (renamed
 * column, wrong type) fails context startup. Kept separate from the other persistence tests so a failure here points
 * directly at a mapping/migration mismatch.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class EntitySchemaValidationTest extends PostgresRepositoryTestBase {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void entitiesMatchMigratedSchema() {
        assertThat(entityManagerFactory.isOpen()).isTrue();
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(3);
    }
}
