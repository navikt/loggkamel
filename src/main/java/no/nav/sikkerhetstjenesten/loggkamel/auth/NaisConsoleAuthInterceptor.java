package no.nav.sikkerhetstjenesten.loggkamel.auth;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class NaisConsoleAuthInterceptor implements ClientHttpRequestInterceptor {

    @Value("${NAIS_SERVICE_ACCOUNT_TOKEN_PATH:#{''}}")
    private String pathToTokenFile;

    @Override
    public @NonNull ClientHttpResponse intercept(HttpRequest request, byte @NonNull [] body, ClientHttpRequestExecution execution) throws IOException {
        String authToken = getAuthToken();
        request.getHeaders().setBearerAuth(authToken);

        return execution.execute(request, body);
    }

    private String getAuthToken() {
        Path filePath = Path.of(pathToTokenFile);

        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new DependencyException("Unable to read nais console token from workflow file", e);
        }
    }
}
