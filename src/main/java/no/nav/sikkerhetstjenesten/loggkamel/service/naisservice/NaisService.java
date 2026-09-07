package no.nav.sikkerhetstjenesten.loggkamel.service.naisservice;

import java.util.List;

public interface NaisService {

    String getCurrentEnvGCPIDForTeam(String naisTeam);

    List<String> getAllNaisteamsForEmail(String email);
}
