package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntraProxyAdapterImplTest {

    private static final String NAV_IDENT = "A123456";
    private static final String T_IDENT = "T654321";

    @Mock
    EntraProxyClient client;

    @InjectMocks
    EntraProxyAdapterImpl adapter;

    @Test
    void getAnsattFraNavIdent_successful() {
        EntraProxyAnsatt ansatt = EntraProxyAnsatt.builder().navIdent(NAV_IDENT).epost("a@nav.no").build();
        when(client.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(ansatt);

        assertEquals(ansatt, adapter.getAnsattFraNavIdent(NAV_IDENT));
    }

    @Test
    void getAnsattFraNavIdent_notFoundReturnsNull() {
        when(client.getAnsattFraNavIdent(NAV_IDENT)).thenThrow(restClientException(HttpStatus.NOT_FOUND));

        assertNull(adapter.getAnsattFraNavIdent(NAV_IDENT));
    }

    @Test
    void getAnsattFraNavIdent_badRequestReturnsNull() {
        when(client.getAnsattFraNavIdent(NAV_IDENT)).thenThrow(restClientException(HttpStatus.BAD_REQUEST));

        assertNull(adapter.getAnsattFraNavIdent(NAV_IDENT));
    }

    @Test
    void getAnsattFraNavIdent_non404ExceptionConvertedToDependencyException() {
        when(client.getAnsattFraNavIdent(NAV_IDENT)).thenThrow(restClientException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(DependencyException.class, () -> adapter.getAnsattFraNavIdent(NAV_IDENT));
    }

    @Test
    void getAnsattFraTIdent_successful() {
        EntraProxyAnsatt ansatt = EntraProxyAnsatt.builder().tident(T_IDENT).epost("t@nav.no").build();
        when(client.getAnsattFraTIdent(T_IDENT)).thenReturn(ansatt);

        assertEquals(ansatt, adapter.getAnsattFraTIdent(T_IDENT));
    }

    @Test
    void getAnsattFraTIdent_notFoundReturnsNull() {
        when(client.getAnsattFraTIdent(T_IDENT)).thenThrow(restClientException(HttpStatus.NOT_FOUND));

        assertNull(adapter.getAnsattFraTIdent(T_IDENT));
    }

    @Test
    void getAnsattFraTIdent_badRequestReturnsNull() {
        when(client.getAnsattFraTIdent(T_IDENT)).thenThrow(restClientException(HttpStatus.BAD_REQUEST));

        assertNull(adapter.getAnsattFraTIdent(T_IDENT));
    }

    @Test
    void getAnsattFraTIdent_non404ExceptionConvertedToDependencyException() {
        when(client.getAnsattFraTIdent(T_IDENT)).thenThrow(restClientException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(DependencyException.class, () -> adapter.getAnsattFraTIdent(T_IDENT));
    }

    private RestClientResponseException restClientException(HttpStatus status) {
        return new RestClientResponseException(
                "entra-proxy failure",
                status.value(),
                status.getReasonPhrase(),
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
    }

}