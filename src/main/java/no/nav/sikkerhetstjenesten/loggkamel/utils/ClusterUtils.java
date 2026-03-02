package no.nav.sikkerhetstjenesten.loggkamel.utils;

import no.nav.sikkerhetstjenesten.loggkamel.bean.PgBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static no.nav.sikkerhetstjenesten.loggkamel.utils.ClusterConstraints.*;

public class ClusterUtils {

    private static final Logger log = LoggerFactory.getLogger(ClusterUtils.class);

    public static String[] getProfiles() {
        String currentAsString = System.getenv(NAIS_CLUSTER_NAME) != null ? System.getenv(NAIS_CLUSTER_NAME) : LOCAL;
        System.out.println("local env: " + currentAsString);
        log.error("local env: " + currentAsString);
        Optional<Cluster> currentOptional = Arrays.stream(Cluster.values()).filter(e -> e.clusterName.equals(currentAsString)).findFirst();

        if (currentOptional.isEmpty()) {
            return new String[]{};
        }

        Cluster current = currentOptional.get();

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

        private final String clusterName;

        Cluster(String clusterName) {
            this.clusterName = clusterName;
        }
    }
}
