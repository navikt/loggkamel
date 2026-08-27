package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.EntraProxyDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class NativeLogLineEnrichmentProcessor {

    static final Logger log = LoggerFactory.getLogger(NativeLogLineEnrichmentProcessor.class);

    static final String ENTRA_PROXY_ERROR_MESSAGE = "Error when fetching ansatt information from entra-proxy";

    final Metrics metrics;
    private final EntraProxyService entraProxyService;
    private final Validator validator;

    public NativeLogLineEnrichmentProcessor(Metrics metrics, EntraProxyService entraProxyService, Validator validator) {
        this.metrics = metrics;
        this.entraProxyService = entraProxyService;
        this.validator = validator;
    }

    String getAnsattEpostFromNavIdent(String navIdent) {
        EntraProxyAnsatt entraProxyAnsatt;
        try {
            entraProxyAnsatt = entraProxyService.getAnsattFraNavIdent(navIdent);
        } catch (Exception e) {
            log.warn(ENTRA_PROXY_ERROR_MESSAGE, e);
            throw new EntraProxyDependencyException(ENTRA_PROXY_ERROR_MESSAGE, e);
        }

        if (entraProxyAnsatt == null || entraProxyAnsatt.getEpost() == null || entraProxyAnsatt.getEpost().isBlank()) {
            log.info("Entra-proxy returned empty response for navIdent {}, not enriching with employee email", navIdent);
            metrics.incrementUnknownNavIdent();
            return null;
        }

        return entraProxyAnsatt.getEpost();
    }

    String getAnsattEpostFromTIdent(String tIdent) {
        EntraProxyAnsatt entraProxyAnsatt;
        try {
            entraProxyAnsatt = entraProxyService.getAnsattFraTIdent(tIdent);
        } catch (Exception e) {
            log.warn(ENTRA_PROXY_ERROR_MESSAGE, e);
            throw new EntraProxyDependencyException(ENTRA_PROXY_ERROR_MESSAGE, e);
        }

        if (entraProxyAnsatt == null || entraProxyAnsatt.getEpost() == null || entraProxyAnsatt.getEpost().isBlank()) {
            log.info("Entra-proxy returned empty response for tIdent {}, not enriching with employee email", tIdent);
            metrics.incrementUnknownNavIdent();
            return null;
        }

        return entraProxyAnsatt.getEpost();
    }

    void validateEnrichedAuditlogg(EnrichedAuditlogg enrichedAuditlogg) {
        Set<ConstraintViolation<EnrichedAuditlogg>> violations = validator.validate(enrichedAuditlogg);
        if (!violations.isEmpty()) {
            throw new InvalidLogLineException("Validation failed: " +
                    violations.stream()
                            .map(v -> v.getPropertyPath() + " " + v.getMessage())
                            .collect(Collectors.joining(", ")));
        }
    }
}
