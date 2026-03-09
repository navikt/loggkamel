package no.nav.sikkerhetstjenesten.loggkamel;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

//TODO: figure out how to re-enable class without running into configuration issues re: MockOAuth2Server
//@CamelSpringBootTest
//@SpringBootTest(classes = LoggkamelApp.class)
//@ActiveProfiles({"local", "test"})
//@EnableMockOAuth2Server
//public class LoggkamelAppTest {
//
//    @Autowired
//    private CamelContext camelContext;
//
//    @Test
//    public void toDo() throws Exception {
//        NotifyBuilder notify = new NotifyBuilder(camelContext).whenDone(1).create();
//        //assertTrue(notify.matches(10, TimeUnit.SECONDS));
//    }
//}
