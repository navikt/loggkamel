package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyClient;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Map;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@Configuration
public class EntraProxyConfig {

    private static final Logger log = LoggerFactory.getLogger(EntraProxyConfig.class);

    @Autowired
    GenericApplicationContext ctxt;

    @Value("${ENTRA_PROXY_BASE_URL}")
    private String entraProxyUrl;

    @Value("${NAIS_TOKEN_ENDPOINT:#{''}}")
    private String naisTokenEndpoint;

    @Bean
    @ConditionalOnGCP
    public EntraProxyClient entraProxyClient() {
        return buildEntraProxyClientWithCurrentToken();
    }

    //TODO: switch back to every 5 minutes after testing
    @Scheduled(fixedRate = 60000)
//    @Scheduled(fixedRate = 300000)
    @ConditionalOnGCP
    private void refreshEntraProxClient() {
        log.info("Refreshing EntraProxyClient bean with new auth token");
        ctxt.registerBean("EntraProxyClient", EntraProxyClient.class, buildEntraProxyClientWithCurrentToken());
        ctxt.refresh();
    }

    private EntraProxyClient buildEntraProxyClientWithCurrentToken() {
        String authToken = getAuthToken();
        //TODO: remove token logging after testing
        log.info("Creating EntraProxyClient with authToken ending in {}", authToken.substring(Math.max(0, authToken.length() - 5)));

        RestClient restClient = RestClient.builder()
                .baseUrl(entraProxyUrl)
                .requestInterceptors(interceptors -> {
                    interceptors.add(new LoggingRequestInterceptor());
                    interceptors.add((request, body, execution) -> {
                        request.getHeaders().setBearerAuth(authToken);
                        return execution.execute(request, body);
                    });
                })
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .build();

        return proxyFactory.createClient(EntraProxyClient.class);
    }

    private String getAuthToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        //TODO: construct target programatically instead of hardcoding
        Map<String, String> body = Map.of(
                "identity_provider", "entra_id",
                "target", "api://dev-gcp.sikkerhetstjenesten.entra-proxy/.default"
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

    @Bean
    @ConditionalOnLocalOrTest
    public EntraProxyClient entraProxyClientMock() {
        return new EntraProxyMock();
    }
}
