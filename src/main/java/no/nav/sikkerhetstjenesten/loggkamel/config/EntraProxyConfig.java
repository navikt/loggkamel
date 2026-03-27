package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyClient;
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
    public EntraProxyClient entraProxyClient(RestClient.Builder restClientBuilder, @Value("${ENTRA_PROXY_BASE_URL}")
                                                String entraProxyUrl) {
        RestClient restClient = restClientBuilder
                .baseUrl(entraProxyUrl)
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .build();

        return proxyFactory.createClient(EntraProxyClient.class);
    }

    @Bean
    @ConditionalOnLocalOrTest
    public EntraProxyClient entraProxyClientMock() {
        return new EntraProxyMock();
    }
}
