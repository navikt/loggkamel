package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;

import java.util.List;

public interface NaisTeamMembershipService {

    String EMAIL_CLAIM = "preferred_username";

    List<String> getAllNaisteamsForEmail(String email);

    static void requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ForbiddenOperationException("Tokenet mangler " + EMAIL_CLAIM + " og kan ikke knyttes til en bruker");
        }
    }
}
