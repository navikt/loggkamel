package no.nav.sikkerhetstjenesten.loggkamel.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EntraProxyAuthInterceptor extends GenericAuthInterceptor {

    @Value("${entra-proxy.namespace:#{''}}")
    private String entraProxyNamespace;

    @Value("${entra-proxy.app-name:#{''}}")
    private String entraProxyAppName;

    @Value("${entra-proxy.cluster:#{''}}")
    private String entraProxyCluster;

    @Override
    protected String getServiceNamespace() {
        return entraProxyNamespace;
    }

    @Override
    protected String getServiceAppName() {
        return entraProxyAppName;
    }

    @Override
    protected String getServiceCluster() {
        return entraProxyCluster;
    }
}
