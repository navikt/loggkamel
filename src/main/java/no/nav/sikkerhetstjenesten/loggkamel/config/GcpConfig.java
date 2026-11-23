package no.nav.sikkerhetstjenesten.loggkamel.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import no.nav.boot.conditionals.ConditionalOnGCP;
import org.apache.camel.component.google.storage.GoogleCloudStorageComponent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnGCP
@EnableConfigurationProperties(ColdStorageProperties.class)
public class GcpConfig {

    @Bean
    public Storage serviceLocalGcpStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean
    @Lazy
    public Storage coldStorage(ColdStorageProperties properties) {
        return StorageOptions.newBuilder().setProjectId(properties.projectId()).build().getService();
    }

    @Bean("google-storage")
    public GoogleCloudStorageComponent googleCloudStorageComponent(@Qualifier("serviceLocalGcpStorage") Storage storage) {
        GoogleCloudStorageComponent component = new GoogleCloudStorageComponent();
        component.setAutowiredEnabled(false);
        component.getConfiguration().setStorageClient(storage);
        component.getConfiguration().setAutoCreateBucket(false);
        return component;
    }
}
