package no.nav.sikkerhetstjenesten.loggkamel.controller;

import no.nav.boot.conditionals.ConditionalOnDev;
import no.nav.boot.conditionals.ConditionalOnDevOrLocal;
import no.nav.security.token.support.spring.UnprotectedRestController;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@UnprotectedRestController("/api/v1")
@ConditionalOnDevOrLocal
public class TestController {

    private final EntraProxyService entraProxyService;

    @Autowired
    public TestController(EntraProxyService entraProxyService) {
        this.entraProxyService = entraProxyService;
    }

    @GetMapping("ansatt/{navIdent}")
    public EntraProxyAnsatt getLoggkamel(@PathVariable("navIdent") String navIdent) {
        return entraProxyService.getAnsattFromNavIdent(navIdent);
    }
}
