package no.nav.sikkerhetstjenesten.loggkamel.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpConfig {

    @Bean
    public Storage gcpStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }
}
