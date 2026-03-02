package no.nav.sikkerhetstjenesten.loggkamel.utils;

public class ClusterConstraints {

    static final String LOCAL = "local";
    static final String GCP = "gcp";
    static final String TEST = "test";
    static final String DEV = "dev";
    static final String PROD = "prod";
    static final String DEV_GCP = "$DEV-$GCP";
    static final String PROD_GCP = "$PROD-$GCP";
    static final String NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME";
}
