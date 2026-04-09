package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyClient;
import no.nav.sikkerhetstjenesten.loggkamel.config.EntraProxyCacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        EntraProxyService.class,
        EntraProxyCacheConfig.class,
        EntraProxyServiceCacheIntegrationTest.TestConfig.class
})
class EntraProxyServiceCacheIntegrationTest {

    private static final String NAV_IDENT = "A123456";
    private static final String T_IDENT = "T654321";
    private static final String EPOST = "epost";

    @Autowired
    EntraProxyService service;

    @Autowired
    EntraProxyClient client;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(client);
        clearCache(EntraProxyCacheConfig.ENTRA_PROXY_BY_NAV_IDENT);
        clearCache(EntraProxyCacheConfig.ENTRA_PROXY_BY_T_IDENT);
    }

    @Test
    void getAnsattFraNavIdent_successfulResponseIsCached() {
        EntraProxyAnsatt ansatt = EntraProxyAnsatt.builder().navIdent(NAV_IDENT).epost(EPOST).build();
        when(client.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(ansatt);

        EntraProxyAnsatt first = service.getAnsattFraNavIdent(NAV_IDENT);
        EntraProxyAnsatt second = service.getAnsattFraNavIdent(NAV_IDENT);

        assertEquals(ansatt, first);
        assertEquals(ansatt, second);
        verify(client, times(1)).getAnsattFraNavIdent(NAV_IDENT);
    }

    @Test
    void getAnsattFraNavIdent_notFoundIsCachedAsNull() {
        when(client.getAnsattFraNavIdent(NAV_IDENT)).thenThrow(restClientException(HttpStatus.NOT_FOUND));

        EntraProxyAnsatt first = service.getAnsattFraNavIdent(NAV_IDENT);
        EntraProxyAnsatt second = service.getAnsattFraNavIdent(NAV_IDENT);

        assertNull(first);
        assertNull(second);
        verify(client, times(1)).getAnsattFraNavIdent(NAV_IDENT);
    }

    @Test
    void getAnsattFraNavIdent_non404ExceptionIsNotCached() {
        when(client.getAnsattFraNavIdent(NAV_IDENT)).thenThrow(restClientException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(RestClientResponseException.class, () -> service.getAnsattFraNavIdent(NAV_IDENT));
        assertThrows(RestClientResponseException.class, () -> service.getAnsattFraNavIdent(NAV_IDENT));

        verify(client, times(2)).getAnsattFraNavIdent(NAV_IDENT);
    }

    @Test
    void getAnsattFraTIdent_successfulResponseIsCached() {
        EntraProxyAnsatt ansatt = EntraProxyAnsatt.builder().tident(T_IDENT).epost(EPOST).build();
        when(client.getAnsattFraTIdent(T_IDENT)).thenReturn(ansatt);

        EntraProxyAnsatt first = service.getAnsattFraTIdent(T_IDENT);
        EntraProxyAnsatt second = service.getAnsattFraTIdent(T_IDENT);

        assertEquals(ansatt, first);
        assertEquals(ansatt, second);
        verify(client, times(1)).getAnsattFraTIdent(T_IDENT);
    }

    @Test
    void getAnsattFraTIdent_notFoundIsCachedAsNull() {
        when(client.getAnsattFraTIdent(T_IDENT)).thenThrow(restClientException(HttpStatus.NOT_FOUND));

        EntraProxyAnsatt first = service.getAnsattFraTIdent(T_IDENT);
        EntraProxyAnsatt second = service.getAnsattFraTIdent(T_IDENT);

        assertNull(first);
        assertNull(second);
        verify(client, times(1)).getAnsattFraTIdent(T_IDENT);
    }

    @Test
    void getAnsattFraTIdent_non404ExceptionIsNotCached() {
        when(client.getAnsattFraTIdent(T_IDENT)).thenThrow(restClientException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(RestClientResponseException.class, () -> service.getAnsattFraTIdent(T_IDENT));
        assertThrows(RestClientResponseException.class, () -> service.getAnsattFraTIdent(T_IDENT));

        verify(client, times(2)).getAnsattFraTIdent(T_IDENT);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        assertNotNull(cache);
        cache.clear();
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

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        EntraProxyClient entraProxyClient() {
            return Mockito.mock(EntraProxyClient.class);
        }
    }
}

