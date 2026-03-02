package no.nav.sikkerhetstjenesten.loggkamel.utils;

import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.utils.ClusterConstraints.*;

public class ClusterUtils {

    public static String[] getProfiles() {
        String currentAsString = System.getenv(NAIS_CLUSTER_NAME) != null ? System.getenv(NAIS_CLUSTER_NAME) : LOCAL;
        Cluster current = Cluster.valueOf(currentAsString);

        if (List.of(Cluster.TEST_CLUSTER, Cluster.LOCAL_CLUSTER).contains(current)) {
            System.setProperty(NAIS_CLUSTER_NAME, current.clusterName);
            return new String[]{current.clusterName};
        } else if (Cluster.DEV_GCP_CLUSTER == current) {
            return new String[]{DEV, DEV_GCP, GCP};
        } else if (Cluster.PROD_GCP_CLUSTER == current) {
            return new String[]{PROD, PROD_GCP, GCP};
        } else {
            return new String[]{};
        }
    }

    public enum Cluster {
        TEST_CLUSTER(TEST),
        LOCAL_CLUSTER(LOCAL),
        DEV_GCP_CLUSTER(DEV_GCP),
        PROD_GCP_CLUSTER(PROD_GCP);

        private String clusterName;

        Cluster(String clusterName) {
            this.clusterName = clusterName;
        }
    }
}
