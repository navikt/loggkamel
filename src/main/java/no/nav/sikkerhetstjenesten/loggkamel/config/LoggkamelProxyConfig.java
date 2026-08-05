package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.sikkerhetstjenesten.loggkamel.auth.LoggkamelProxyAuthInterceptor;
import no.nav.sikkerhetstjenesten.loggkamel.client.LoggkamelProxyClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class LoggkamelProxyConfig {

    @Value("${LOGGKAMEL_PROXY_BASE_URL}")
    private String loggkamelProxyBaseUrl;

    @Bean
    public LoggkamelProxyClient loggkamelProxyClient(LoggkamelProxyAuthInterceptor loggkamelProxyAuthInterceptor) {
        RestClient restClient = RestClient.builder()
                .baseUrl(loggkamelProxyBaseUrl)
                .requestInterceptors(interceptors -> {
                    interceptors.add(new LoggingRequestInterceptor());
                    interceptors.add(loggkamelProxyAuthInterceptor);
                })
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .build();

        return proxyFactory.createClient(LoggkamelProxyClient.class);
    }
}
