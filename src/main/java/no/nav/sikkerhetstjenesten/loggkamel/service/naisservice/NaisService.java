package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;

import java.util.List;

public interface NaisService {

    String EMAIL_CLAIM = "preferred_username";

    String getCurrentEnvGCPIDForTeam(String naisTeam);

    List<String> getAllNaisteamsForEmail(String email);

    static void requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ForbiddenOperationException("Tokenet mangler " + EMAIL_CLAIM + " og kan ikke knyttes til en bruker");
        }
    }
}
