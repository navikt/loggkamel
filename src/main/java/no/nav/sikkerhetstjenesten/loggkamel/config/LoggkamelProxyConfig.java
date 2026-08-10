package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.auth.LoggkamelProxyAuthInterceptor;
import no.nav.sikkerhetstjenesten.loggkamel.client.*;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.concurrent.TimeUnit;

@Configuration
public class LoggkamelProxyConfig {

    @Value("${LOGGKAMEL_PROXY_BASE_URL}")
    private String loggkamelProxyBaseUrl;

    @Bean
    @ConditionalOnGCP
    public LoggkamelProxyClient loggkamelProxyClient(LoggkamelProxyAuthInterceptor loggkamelProxyAuthInterceptor) {

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setIdleTimeout(Timeout.ofMinutes(55))
                .setTimeToLive(59, TimeUnit.MINUTES)
                .build();

        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .evictExpiredConnections()
                .build();

        RestClient restClient = RestClient.builder()
                .baseUrl(loggkamelProxyBaseUrl)
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
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

    @Bean
    @ConditionalOnGCP
    public LoggkamelProxyAdapter loggkamelProxyAdapter(LoggkamelProxyClient loggkamelProxyClient) {
        return new LoggkamelProxyAdapterImpl(loggkamelProxyClient);
    }

    @Bean
    @ConditionalOnLocalOrTest
    public LoggkamelProxyAdapter mockLoggkamelProxyAdapter() {
        return new LoggkamelProxyAdapterMock();
    }
}
