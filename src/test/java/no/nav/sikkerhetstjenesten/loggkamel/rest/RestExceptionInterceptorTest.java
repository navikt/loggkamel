package no.nav.sikkerhetstjenesten.loggkamel.rest;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class RestExceptionInterceptorTest {

    private static final String REQUEST_URI = "some/request/uri";

    private final RestExceptionInterceptor restExceptionInterceptor = new RestExceptionInterceptor();

    @Test
    void shouldMapMessageNotReadableExceptionToForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(REQUEST_URI);

        HttpMessageNotReadableException mockedException = Mockito.mock(HttpMessageNotReadableException.class);
        when(mockedException.getMessage()).thenReturn("no");
        when(mockedException.getLocalizedMessage()).thenReturn("nei");

        var response = restExceptionInterceptor.handleHttpMessageNotReadableException(mockedException, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().errorCode());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), response.getBody().message());
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    void shouldMapForbiddenOperationExceptionToForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(REQUEST_URI);

        var response = restExceptionInterceptor.handleForbiddenOperationException(new ForbiddenOperationException("no"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getBody().errorCode());
        assertEquals(HttpStatus.FORBIDDEN.getReasonPhrase(), response.getBody().message());
        assertEquals(REQUEST_URI, response.getBody().path());
    }

    @Test
    void shouldMapUpdatingNonexistentTaskExceptionToConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(REQUEST_URI);

        var response = restExceptionInterceptor.handleUpdatingNonexistentTaskException(new UpdatingNonexistentTaskException("why"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().errorCode());
        assertEquals(HttpStatus.CONFLICT.getReasonPhrase(), response.getBody().message());
        assertEquals(REQUEST_URI, response.getBody().path());
    }
}

