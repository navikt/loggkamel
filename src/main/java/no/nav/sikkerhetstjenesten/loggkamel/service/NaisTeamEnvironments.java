package no.nav.sikkerhetstjenesten.loggkamel.service;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class NaisTeamEnvironments {

    @NonNull
    List<GCPProject> environments;
}
