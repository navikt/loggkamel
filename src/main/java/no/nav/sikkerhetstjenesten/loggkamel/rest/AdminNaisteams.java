package no.nav.sikkerhetstjenesten.loggkamel.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "auth")
public record AdminNaisteams(List<String> adminTeams) {
}
