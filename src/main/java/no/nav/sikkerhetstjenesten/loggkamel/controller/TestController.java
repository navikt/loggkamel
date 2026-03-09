package no.nav.sikkerhetstjenesten.loggkamel.controller;

import no.nav.boot.conditionals.ConditionalOnDevOrLocal;
import no.nav.security.token.support.spring.UnprotectedRestController;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@UnprotectedRestController("/api/v1")
@ConditionalOnDevOrLocal
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    private final EntraProxyService entraProxyService;

    @Autowired
    public TestController(EntraProxyService entraProxyService) {
        this.entraProxyService = entraProxyService;
    }

    @GetMapping("ansatt/{navIdent}")
    public EntraProxyAnsatt getAnsattForNavident(@PathVariable("navIdent") String navIdent) {
        log.info("Fetching Ansatt for navIdent: {}", navIdent);
        return entraProxyService.getAnsattFromNavIdent(navIdent);
    }
}
