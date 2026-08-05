package no.nav.sikkerhetstjenesten.loggkamel.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoggkamelProxyAuthInterceptor extends GenericAuthInterceptor {

    @Value("${loggkamel-proxy.namespace:#{''}}")
    private String loggkamelProxyNamespace;

    @Value("${loggkamel-proxy.app-name:#{''}}")
    private String loggkamelProxyAppName;

    @Value("${loggkamel-proxy.cluster:#{''}}")
    private String loggkamelProxyCluster;

    String getServiceNamespace() {
        return loggkamelProxyNamespace;
    }

    String getServiceAppName() {
        return loggkamelProxyAppName;
    }

    String getServiceCluster() {
        return loggkamelProxyCluster;
    }
}
