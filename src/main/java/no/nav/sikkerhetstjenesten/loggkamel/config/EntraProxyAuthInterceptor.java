package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@Component
public class EntraProxyAuthInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(EntraProxyAuthInterceptor.class);

    @Value("${NAIS_CLUSTER_NAME:#{''}}")
    private String clusterName;

    @Value("${entra-proxy.namespace:#{''}}")
    private String entraProxyNamespace;

    @Value("${entra-proxy.app-name:#{''}}")
    private String entraProxyAppName;

    @Value("${NAIS_TOKEN_ENDPOINT:#{''}}")
    private String naisTokenEndpoint;

    @Override
    public @NonNull ClientHttpResponse intercept(HttpRequest request, byte @NonNull [] body, ClientHttpRequestExecution execution) throws IOException {

        String authToken = getAuthToken();
        request.getHeaders().setBearerAuth(authToken);

        return execution.execute(request, body);
    }

    private String getAuthToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String target = String.format("api://%s.%s.%s/.default", clusterName, entraProxyNamespace, entraProxyAppName);
        Map<String, String> body = Map.of(
                "identity_provider", "entra_id",
                "target", target
        );

        ResponseEntity<Map> response = RestClient.create().post()
                .uri(naisTokenEndpoint)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        if (response.getBody() == null || response.getBody().get("access_token") == null) {
            throw new RuntimeException("Tomt svar fra EntraID token-endepunkt");
        }

        return (String) response.getBody().get("access_token");
    }
}
