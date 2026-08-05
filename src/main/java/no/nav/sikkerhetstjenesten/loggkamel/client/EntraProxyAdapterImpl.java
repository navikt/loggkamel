package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

public class EntraProxyAdapterImpl implements EntraProxyAdapter {

    private final EntraProxyClient client;

    public EntraProxyAdapterImpl(EntraProxyClient client) {
        this.client = client;
    }

    public EntraProxyAnsatt getAnsattFraNavIdent(String navIdent) {
        try {
            return client.getAnsattFraNavIdent(navIdent);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return null;
            }

            throw new DependencyException(e.getMessage(), e);
        }
    }

    public EntraProxyAnsatt getAnsattFraTIdent(String tIdent) {
        try {
            return client.getAnsattFraTIdent(tIdent);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return null;
            }

            throw new DependencyException(e.getMessage(), e);
        }
    }
}
