package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAdapterImpl;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyClient;
import no.nav.sikkerhetstjenesten.loggkamel.client.MockEntraProxyAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class EntraProxyConfig {

    @Value("${ENTRA_PROXY_BASE_URL}")
    private String entraProxyUrl;

    @Bean
    public EntraProxyClient entraProxyClient(EntraIdAuthInterceptor entraIdAuthInterceptor) {
        RestClient restClient = RestClient.builder()
                .baseUrl(entraProxyUrl)
                .requestInterceptors(interceptors -> {
                    interceptors.add(new LoggingRequestInterceptor());
                    interceptors.add(entraIdAuthInterceptor);
                })
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .build();

        return proxyFactory.createClient(EntraProxyClient.class);
    }

    @Bean
    @ConditionalOnGCP
    public EntraProxyAdapter entraProxyAdapter(EntraProxyClient entraProxyClient) {
        return new EntraProxyAdapterImpl(entraProxyClient);
    }

    @Bean
    @ConditionalOnLocalOrTest
    public EntraProxyAdapter mockEntraProxyAdapter() {
        return new MockEntraProxyAdapter();
    }
}
