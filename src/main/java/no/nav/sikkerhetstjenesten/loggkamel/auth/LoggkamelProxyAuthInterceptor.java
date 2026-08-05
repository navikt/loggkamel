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

    @Override
    protected String getServiceNamespace() {
        return loggkamelProxyNamespace;
    }

    @Override
    protected String getServiceAppName() {
        return loggkamelProxyAppName;
    }

    @Override
    protected String getServiceCluster() {
        return loggkamelProxyCluster;
    }
}
