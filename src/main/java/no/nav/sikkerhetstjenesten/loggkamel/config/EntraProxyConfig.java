package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class EntraProxyConfig {

    @Bean
    public EntraProxyInterface entraProxyClient(@Value("${ENTRA_PROXY_BASE_URL}")
                                                String entraProxyUrl) {
        RestClient restClient = RestClient.create(entraProxyUrl);

        // Create factory for client proxies
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .build();

        return proxyFactory.createClient(EntraProxyInterface.class);
    }
}
