package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import jakarta.validation.Validator;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.StandardizedLogLineFilter.MESSAGE_SHOULD_BE_SKIPPED;

@Service
public class PostgresLogLineEnrichmentProcessor extends NativeLogLineEnrichmentProcessor {
    private static final Logger log = LoggerFactory.getLogger(PostgresLogLineEnrichmentProcessor.class);

    static final List<String> PG_AUDIT_MESSAGE_LEVELS = List.of("DEBUG5", "DEBUG4", "DEBUG3", "DEBUG2", "DEBUG1", "INFO", "NOTICE", "WARNING", "ERROR", "LOG", "FATAL", "PANIC");
    static final List<String> PG_AUDIT_CONTEXT_LABELS = List.of("HINT", "STATEMENT", "DETAIL", "CONTEXT");

    static final String UNEXPECTED_LOG_PATTERN_MESSAGE = "Log failed to match expected pattern, cannot extract enrichment attributes";
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS zzz", Locale.ENGLISH);

    @Autowired
    public PostgresLogLineEnrichmentProcessor(EntraProxyService entraProxyService,
                                              Metrics metrics,
                                              Validator validator) {
        super(metrics, entraProxyService, validator);
    }

    public void enrich(Exchange exchange) {
        String body = exchange.getMessage().getBody(AuditloggLineMessage.class).getBody();

        if (body == null || body.isBlank()) {
            throw new InvalidPostgresLogLineException("Audit log message is blank");
        }

        if (messageContainsPostgresNonLogStatement(body)) {
            exchange.setVariable(MESSAGE_SHOULD_BE_SKIPPED, true);
            exchange.getMessage().setBody(null);
            return;
        }

        EnrichedAuditlogg enrichedAuditlogg;
        try {
            enrichedAuditlogg = extractEnrichmentFromLog(body);
        } catch (RuntimeException e) {
            throw new InvalidPostgresLogLineException("Failure converting values extracted from log line into EnrichedAuditlogg", e);
        }
        enrichedAuditlogg.setEpost(getAnsattEpostFromNavIdent(enrichedAuditlogg.getNavIdent()));

        validateEnrichedAuditlogg(enrichedAuditlogg);
        exchange.getMessage().setBody(enrichedAuditlogg);
    }

    private boolean messageContainsPostgresNonLogStatement(String body) {
        if (body.contains(" LOG: ")) {
            return false;
        }

        if (PG_AUDIT_MESSAGE_LEVELS.stream().anyMatch(level -> body.contains(" " + level + ": "))) {
            return true;
        }

        if (PG_AUDIT_CONTEXT_LABELS.stream().anyMatch(label -> body.contains(" " + label + ": "))) {
            return true;
        }

        return false;
    }

    private EnrichedAuditlogg extractEnrichmentFromLog(String body) {
        String regex = "^(.*):.*:(.*)@(.*?):.*(SESSION|OBJECT),(.*),(.*),(READ|WRITE|FUNCTION|ROLE|DDL|MISC|MISC_SET),(.*?),(.*?),(.*?),(\"|)?([\\s\\S]*)\\11,(\"|)?(.*)\\13";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        if (!matcher.find()) {
            log.warn(UNEXPECTED_LOG_PATTERN_MESSAGE);
            log.debug("Log failed to match expected pattern, cannot extract enrichment attributes. Failing log line: {}", body);
            throw new InvalidPostgresLogLineException(UNEXPECTED_LOG_PATTERN_MESSAGE);
        }

        ZonedDateTime logTime = ZonedDateTime.parse(matcher.group(1), DATE_TIME_FORMATTER);

        String userIdentity = matcher.group(2);
        // if the user identity is of the form "v-oidc-{navIdent}-something", extract the navIdent part. If it isn't of that form, then pass the full string to entra-proxy
        if (userIdentity.startsWith("v-oidc-")) {
            userIdentity = userIdentity.split("-")[2];
        }

        return EnrichedAuditlogg.builder()
                .originalMessage(body)
                .logTime(logTime)
                .navIdent(userIdentity)
                .dbName(matcher.group(3))
                .auditType(EnrichedAuditlogg.AuditType.valueOf(matcher.group(4)))
                .statementId(matcher.group(5))
                .substatementId(matcher.group(6))
                .pgAuditClass(EnrichedAuditlogg.AuditClass.valueOf(matcher.group(7)))
                .pgCommand(matcher.group(8))
                .pgObjectType(matcher.group(9))
                .pgObjectName(matcher.group(10))
                .sqlStatement(matcher.group(12))
                .sqlParameters(matcher.group(14))
                .build();
    }

}