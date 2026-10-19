package no.nav.sikkerhetstjenesten.loggkamel.testsupport;

import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * One Postgres container for the whole test run. It starts the first time this class is used and is stopped by
 * Testcontainers when the JVM exits. Tests that can reuse a database share {@link #jdbcUrl()} and clean up after
 * themselves. Tests that need an empty database create a throwaway one with {@link #createDatabase(String)}.
 * <p>
 * Only touch this class from tests guarded by {@link DockerAvailableCondition}, since loading it requires Docker.
 */
public final class SharedPostgres {

    // Matches the production Postgres version (see .nais/*.json)
    private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer("postgres:18-alpine");

    static {
        CONTAINER.start();
    }

    private SharedPostgres() {
    }

    public static String jdbcUrl() {
        return CONTAINER.getJdbcUrl();
    }

    public static String username() {
        return CONTAINER.getUsername();
    }

    public static String password() {
        return CONTAINER.getPassword();
    }

    // Identifiers cannot be bound as parameters. Names are chosen by test code, never by external input.
    /** Creates an empty database in the shared container and returns its JDBC url. */
    public static String createDatabase(String name) {
        executeAsAdmin("CREATE DATABASE " + name);
        return "jdbc:postgresql://" + CONTAINER.getHost() + ":" + CONTAINER.getMappedPort(5432) + "/" + name;
    }

    public static void dropDatabase(String name) {
        executeAsAdmin("DROP DATABASE IF EXISTS " + name + " WITH (FORCE)");
    }

    private static void executeAsAdmin(String sql) {
        try (Statement statement = DriverManager.getConnection(jdbcUrl(), username(), password()).createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to run admin statement on shared test database", e);
        }
    }
}
