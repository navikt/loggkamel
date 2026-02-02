package no.nav.sikkerhetstjenesten.loggkamel;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CamelSpringBootTest
@SpringBootTest(classes = LoggkamelApp.class)
public class LoggkamelAppTest {

    @Autowired
    private CamelContext camelContext;

    @Test
    public void toDo() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(camelContext).whenDone(1).create();
        //assertTrue(notify.matches(10, TimeUnit.SECONDS));
    }
}
