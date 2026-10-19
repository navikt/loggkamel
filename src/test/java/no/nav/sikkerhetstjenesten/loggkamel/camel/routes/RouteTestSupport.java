package no.nav.sikkerhetstjenesten.loggkamel.camel.routes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.LoggkamelApp;
import no.nav.sikkerhetstjenesten.loggkamel.testsupport.DockerAvailableCondition;
import no.nav.sikkerhetstjenesten.loggkamel.testsupport.SharedPostgres;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real Camel routes (local variants) against the shared Postgres container from {@link SharedPostgres}. Only the outbound dependencies
 * (entra-proxy and the auditlogg task service) are mocked. All files live in a per-class temp directory.
 */
@SpringBootTest(classes = LoggkamelApp.class)
// Generally only runs in the pipeline, can also be run locally if the developer sets up docker
@ExtendWith(DockerAvailableCondition.class)
// Test using the local profile, skips GCP read/write operations and external service calls (mocks instead)
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class RouteTestSupport {

    @TempDir
    static Path root;

    static Path consumerDir;
    static Path packetDir;
    static Path outputDir;
    static Path streamBackoutDir;
    static Path packetBackoutDir;
    static Path stagingDir;

    @Autowired
    CamelContext camelContext;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    EntraProxyService entraProxyService;

    @BeforeAll
    static void createDirectories() throws IOException {
        consumerDir = Files.createDirectories(root.resolve("postgres-consumer"));
        packetDir = Files.createDirectories(root.resolve("packets"));
        outputDir = Files.createDirectories(root.resolve("output"));
        streamBackoutDir = Files.createDirectories(root.resolve("postgres-backout"));
        packetBackoutDir = Files.createDirectories(root.resolve("packet-backout"));
        stagingDir = Files.createDirectories(root.resolve("staging"));
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgres::jdbcUrl);
        registry.add("spring.datasource.username", SharedPostgres::username);
        registry.add("spring.datasource.password", SharedPostgres::password);

        // The local profile disables Flyway; the tests need the real schema
        registry.add("spring.flyway.enabled", () -> "true");

        String pollingOptions = "?initialDelay=0&delay=100";
        registry.add("routing.postgres.consumer", () -> "file:" + root.resolve("postgres-consumer") + pollingOptions);
        registry.add("routing.postgres.invalid-message", () -> "file:" + root.resolve("postgres-backout"));
        registry.add("routing.packet.bucket", () -> "file:" + root.resolve("packets") + pollingOptions);
        registry.add("routing.packet.invalid-message", () -> "file:" + root.resolve("packet-backout"));
        registry.add("routing.logline.producer", () -> "file:" + root.resolve("output"));
        registry.add("routing.error.redelivery-delay-ms", () -> "0");

        // Routes are started by the tests, never by the schedulers
        registry.add("scheduled.route.control.cron", () -> "-");
        registry.add("scheduled.idempotent-repository.cleanup.cron", () -> "-");
        registry.add("scheduled.idempotent-repository.cleanup.postgres-retention-minutes", () -> "5");
        registry.add("scheduled.idempotent-repository.cleanup.log-packet-retention-minutes", () -> "5");
        registry.add("scheduled.db2.backfill.cron", () -> "-");
        registry.add("scheduled.db2.pull.cron", () -> "-");
        registry.add("pull-rerun.admin-teams", () -> "testteam");
    }

    @BeforeEach
    void resetState() {
        Stream.of(consumerDir, packetDir, outputDir, streamBackoutDir, packetBackoutDir)
                .forEach(RouteTestSupport::emptyDirectory);
        //noinspection SqlWithoutWhere
        jdbcTemplate.update("DELETE FROM camel_messageprocessed");
    }

    void startRoute(String routeId) throws Exception {
        camelContext.getRouteController().startRoute(routeId);
    }

    /** Drops a file into a consumed directory, then waits until the route has finished with it. */
    void dropFileAndAwaitRoute(String routeId, Path directory, String filename, String content) throws IOException {
        dropFileAndAwaitRoute(routeId, directory, filename, content.getBytes(StandardCharsets.UTF_8));
    }

    void dropFileAndAwaitRoute(String routeId, Path directory, String filename, byte[] content) {
        dropFilesAndAwaitRoute(routeId, 1, () -> dropFile(directory, filename, content));
        // The route-done notification can arrive before the file consumer commits its deletion.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertFalse(Files.exists(directory.resolve(filename)), "source file should have been consumed"));
    }

    void dropFilesAndAwaitRoute(String routeId, int exchangeCount, ThrowingRunnable drop) {
        NotifyBuilder done = new NotifyBuilder(camelContext).fromRoute(routeId).whenDone(exchangeCount).create();
        try {
            drop.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertTrue(done.matches(30, TimeUnit.SECONDS), "Route " + routeId + " did not finish " + exchangeCount + " exchange(s) in time");
    }

    // The file is moved into place atomically so the consumer never reads a half-written file
    void dropFile(Path directory, String filename, byte[] content) throws IOException {
        Path staged = Files.write(stagingDir.resolve(filename + "-" + System.nanoTime()), content);
        Files.move(staged, directory.resolve(filename), StandardCopyOption.ATOMIC_MOVE);
    }

    static List<String> regularFileNames(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    JsonNode readJson(Path file) throws IOException {
        return objectMapper.readTree(Files.readString(file));
    }

    int claimCount(String processorName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM camel_messageprocessed WHERE processorname = ?", Integer.class, processorName);
        return count == null ? 0 : count;
    }

    private static void emptyDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            //noinspection ResultOfMethodCallIgnored
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(directory))
                    .forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws IOException;
    }
}
