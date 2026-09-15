package no.nav.sikkerhetstjenesten.loggkamel.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "pull-rerun")
public record PullRerunAdminProperties(List<String> adminTeams) {
}
