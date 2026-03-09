package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyInterface;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyMock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class EntraProxyConfig {

    @Bean
    @ConditionalOnGCP
    public EntraProxyInterface entraProxyClient(RestClient.Builder restClientBuilder, @Value("${ENTRA_PROXY_BASE_URL}")
                                                String entraProxyUrl) {
        RestClient restClient = restClientBuilder
                .baseUrl(entraProxyUrl)
                .build();

        // Create factory for client proxies
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .build();

        return proxyFactory.createClient(EntraProxyInterface.class);
    }

    @Bean
    @ConditionalOnLocalOrTest
    public EntraProxyInterface entraProxyClientMock() {
        return new EntraProxyMock();
    }
}
