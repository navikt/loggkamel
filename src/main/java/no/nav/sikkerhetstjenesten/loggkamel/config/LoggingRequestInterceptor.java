package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingRequestInterceptor.class);

    //TODO: reduce or eliminate logging to avoid logging request tokens
    @Override
    public @NonNull ClientHttpResponse intercept(HttpRequest request, byte @NonNull [] body, ClientHttpRequestExecution execution) throws IOException {
        log.info("Headers for {}: {}", request.getURI(), request.getHeaders());
        log.info("Body for {} {} : {} ", request.getMethod(), request.getURI(), new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response = execution.execute(request, body);
        log.info("Response status for {} {}: {}", request.getMethod(), request.getURI(), response.getStatusCode());

        return response;
    }
}
