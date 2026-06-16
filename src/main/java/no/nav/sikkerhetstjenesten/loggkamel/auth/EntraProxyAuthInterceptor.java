package no.nav.sikkerhetstjenesten.loggkamel.auth;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
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
        String target = String.format("api://%s.%s.%s/.default", clusterName, entraProxyNamespace, entraProxyAppName);
        Map<String, String> authRequestBody = Map.of(
                "identity_provider", "entra_id",
                "target", target
        );

        ResponseEntity<Map> authResponse = RestClient.create().post()
                .uri(naisTokenEndpoint)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(authRequestBody)
                .retrieve()
                .toEntity(Map.class);

        if (authResponse.getBody() == null || authResponse.getBody().get("access_token") == null) {
            throw new RuntimeException("Tomt svar fra EntraID token-endepunkt");
        }

        return (String) authResponse.getBody().get("access_token");
    }
}
