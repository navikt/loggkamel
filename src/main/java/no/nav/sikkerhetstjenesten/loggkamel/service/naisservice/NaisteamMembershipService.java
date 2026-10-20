package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import no.nav.sikkerhetstjenesten.loggkamel.rest.ForbiddenOperationException;

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.auth.NaisTokenIntrospector.EMAIL_CLAIM;

public interface NaisteamMembershipService {

    List<String> getAllNaisteamsForEmail(String email);

    static void requireEmail(String email) {
        if (email == null) {
            throw new ForbiddenOperationException("Tokenet mangler " + EMAIL_CLAIM + " og kan ikke knyttes til en bruker");
        }
    }
}
