package no.nav.sikkerhetstjenesten.loggkamel.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cold-storage.db2")
public record DB2ColdStorageProperties(
        @NotBlank String projectId,
        @NotBlank String bucketName,
        @NotBlank String environment) {
}
