package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.EntraProxyDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NativeLogLineEnrichmentProcessorTest {

    private static final String NAV_IDENT = "navIdent";
    private static final String EPOST = "epost@blah.bleh";

    @Mock
    EntraProxyAnsatt entraProxyAnsatt;

    @Mock
    EnrichedAuditlogg  enrichedAuditlogg;

    @Mock
    ConstraintViolation<EnrichedAuditlogg> violation;

    @Mock
    Path path;

    @Mock
    Metrics metrics;

    @Mock
    EntraProxyService entraProxyService;

    @Mock
    Validator validator;

    NativeLogLineEnrichmentProcessor nativeLogLineEnrichmentProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        nativeLogLineEnrichmentProcessor = new NativeLogLineEnrichmentProcessor(metrics, entraProxyService, validator) {};
    }

    @Test
    void getAnsattEpost_convertsEntraProxyException() {
        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenThrow(new RuntimeException());

        assertThrows(EntraProxyDependencyException.class, () -> nativeLogLineEnrichmentProcessor.getAnsattEpost(NAV_IDENT));
    }

    @Test
    void getAnsattEpost_emptyEmployeeIncrementsMetricsAndReturnsNull() {
        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn("");

        assertNull(nativeLogLineEnrichmentProcessor.getAnsattEpost(NAV_IDENT));

        verify(metrics).incrementUnknownNavIdent();
    }

    @Test
    void getAnsattEpost_happyPath() {
        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(EPOST);

        assertEquals(EPOST, nativeLogLineEnrichmentProcessor.getAnsattEpost(NAV_IDENT));
    }

    @Test
    void validateEnrichedAuditlogg_validationExceptionsFound() {
        when(validator.validate(enrichedAuditlogg)).thenReturn(Set.of(violation));
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("message");

        assertThrows(InvalidLogLineException.class, () -> nativeLogLineEnrichmentProcessor.validateEnrichedAuditlogg(enrichedAuditlogg));
    }

    @Test
    void validateEnrichedAuditlogg_noValidationExceptionsFound() {
        when(validator.validate(enrichedAuditlogg)).thenReturn(Collections.emptySet());

        nativeLogLineEnrichmentProcessor.validateEnrichedAuditlogg(enrichedAuditlogg);
    }

}