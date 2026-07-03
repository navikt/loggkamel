package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogGroupException;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Service
public class NativeLogStreamSplitterProcessor {

    private static final Logger log = LoggerFactory.getLogger(NativeLogStreamSplitterProcessor.class);

    static final String LOG_PACKET_EXTENSION = ".packet";

    public void prepareLogLineHeaders(Exchange exchange) {
        String logGroupFilename = exchange.getMessage().getHeader(FILE_NAME, String.class);

        if (logGroupFilename == null || logGroupFilename.isEmpty()) {
            log.warn("Filename header is missing while splitting log group");
            throw new InvalidLogGroupException("Filename header is missing while splitting log group");
        }

        String logLineListFilename = createFilenameWithUUID(logGroupFilename);
        log.debug("New filename being assigned: {}", logLineListFilename);

        exchange.getMessage().setHeader(FILE_NAME, logLineListFilename);
        exchange.getMessage().setHeader(OBJECT_NAME, logLineListFilename);
    }

    private String createFilenameWithUUID(String originalFileName) {
        String fileExtension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";
        String fileBeforeExtension = fileExtension.isEmpty() ? originalFileName : originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        return fileBeforeExtension + "." + UUID.randomUUID() + fileExtension + LOG_PACKET_EXTENSION;
    }
}

