package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZonedDateTime;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.util.GCPTimestampProvider.MAX_LOG_AGE_IN_DAYS;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GCPTimestampProviderTest {

    private static final ZonedDateTime RECENT = ZonedDateTime.now().minusMinutes(2);
    private static final ZonedDateTime STALE = ZonedDateTime.now().minusDays(MAX_LOG_AGE_IN_DAYS + 1);

    @InjectMocks
    private GCPTimestampProvider gcpTimestampProvider;

    @Test
    void getTimestampFromLogTime_staleLogsGetFreshTimestamp() {
        Instant providedInstant = gcpTimestampProvider.getTimestampFromLogTime(STALE);

        assertTrue(providedInstant.isAfter(STALE.toInstant()));
        assertTrue(providedInstant.isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void getTimestampFromLogTime_recentLogsKeepTheirTimestamp() {
        Instant providedInstant = gcpTimestampProvider.getTimestampFromLogTime(RECENT);

        assertEquals(RECENT.toInstant(), providedInstant);
    }

}