package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.channels.Channels;

import static org.apache.camel.Exchange.FILE_NAME;

@Service
public class InputStreamReader {

    private static final Logger log = LoggerFactory.getLogger(InputStreamReader.class);

    public void prepareBodyAsInputStream(Exchange exchange) {
        Object body = exchange.getMessage().getBody();

        if (body instanceof InputStream) {
            log.debug("Received input stream as InputStream");
            return;
        }

        if (body instanceof Blob blob) {
            log.debug("Received input stream as Blob");
            ReadChannel reader = blob.reader();
            exchange.getMessage().setBody(Channels.newInputStream(reader));
            return;
        }

        // If the body isn't an input stream but can at least be converted to one by camel, we coerce that conversion
        InputStream inputStream = exchange.getMessage().getBody(InputStream.class);
        if (inputStream == null) {
            throw new InvalidPostgresLogGroupException("Unable to convert message body to InputStream for file " + exchange.getIn().getHeader(FILE_NAME, String.class));
        }
        log.debug("Converting message body to InputStream");
        exchange.getMessage().setBody(inputStream);
    }
}
