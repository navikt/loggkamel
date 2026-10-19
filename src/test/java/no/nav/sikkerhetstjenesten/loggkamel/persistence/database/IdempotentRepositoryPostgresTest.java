package no.nav.sikkerhetstjenesten.loggkamel.persistence.database;

import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.LOG_PACKET_CONSUMER;
import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.POSTGRES_CONSUMER;
import static org.assertj.core.api.Assertions.assertThat;

class IdempotentRepositoryPostgresTest extends PostgresRepositoryTestBase {

    @Autowired
    @Qualifier("postgresLogStreamIdempotentRepository")
    JdbcMessageIdRepository streamRepository;

    @Autowired
    @Qualifier("logPacketIdempotentRepository")
    JdbcMessageIdRepository packetRepository;

    @Autowired
    IdempotentMessageRepository cleanupRepository;

    @BeforeEach
    void startRepositories() {
        streamRepository.start();
        packetRepository.start();
    }

    @Test
    void firstClaimSucceedsAndDuplicateIsRejected() {
        assertThat(streamRepository.add("testdb.20260115.log")).isTrue();
        assertThat(streamRepository.add("testdb.20260115.log")).isFalse();
        assertThat(streamRepository.contains("testdb.20260115.log")).isTrue();
    }

    @Test
    void claimsAreScopedPerProcessor() {
        assertThat(streamRepository.add("shared-name")).isTrue();

        assertThat(packetRepository.contains("shared-name")).isFalse();
        assertThat(packetRepository.add("shared-name")).isTrue();
    }

    @Test
    void removedClaimCanBeMadeAgain() {
        streamRepository.add("testdb.20260115.log");

        assertThat(streamRepository.remove("testdb.20260115.log")).isTrue();
        assertThat(streamRepository.add("testdb.20260115.log")).isTrue();
    }

    @Test
    void cleanupDeletesOnlyOldClaimsForTheGivenProcessor() {
        Instant now = Instant.now();
        insert(POSTGRES_CONSUMER, "old", now.minus(10, ChronoUnit.DAYS));
        insert(POSTGRES_CONSUMER, "recent", now.minus(1, ChronoUnit.HOURS));
        insert(LOG_PACKET_CONSUMER, "old-other-processor", now.minus(10, ChronoUnit.DAYS));

        int deleted = cleanupRepository.deleteMessagesOlderThan(POSTGRES_CONSUMER, now.minus(5, ChronoUnit.DAYS));

        assertThat(deleted).isEqualTo(1);
        assertThat(streamRepository.contains("old")).isFalse();
        assertThat(streamRepository.contains("recent")).isTrue();
        assertThat(packetRepository.contains("old-other-processor")).isTrue();
    }

    private void insert(String processor, String messageId, Instant createdAt) {
        jdbc.update("INSERT INTO camel_messageprocessed (processorname, messageid, createdat) VALUES (?, ?, ?)",
                processor, messageId, Timestamp.from(createdAt));
    }
}
