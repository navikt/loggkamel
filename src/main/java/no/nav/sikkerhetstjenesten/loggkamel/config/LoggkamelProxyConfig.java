package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.sikkerhetstjenesten.loggkamel.auth.EntraProxyAuthInterceptor;
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

    //TODO: replace with a new LoggkamelProxyAuthInterceptor, or put together a more general solution
    @Bean
    public LoggkamelProxyClient loggkamelProxyClient(EntraProxyAuthInterceptor entraProxyAuthInterceptor) {
        RestClient restClient = RestClient.builder()
                .baseUrl(loggkamelProxyBaseUrl)
                .requestInterceptors(interceptors -> {
                    interceptors.add(new LoggingRequestInterceptor());
                    interceptors.add(entraProxyAuthInterceptor);
                })
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .build();

        return proxyFactory.createClient(LoggkamelProxyClient.class);
    }
}
