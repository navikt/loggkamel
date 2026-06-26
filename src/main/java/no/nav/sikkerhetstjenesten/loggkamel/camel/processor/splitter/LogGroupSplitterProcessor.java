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
public class LogGroupSplitterProcessor {

    private static final Logger log = LoggerFactory.getLogger(LogGroupSplitterProcessor.class);

    public void prepareLogLineHeaders(Exchange exchange) {
        String originalFileName = exchange.getMessage().getHeader(FILE_NAME, String.class);

        if (originalFileName == null || originalFileName.isEmpty()) {
            log.warn("Filename header is missing while splitting log group");
            throw new InvalidLogGroupException("Filename header is missing while splitting log group");
        }

        String filenameWithUUID = createFilenameWithUUID(originalFileName);
        log.debug("New filename being assigned: {}", filenameWithUUID);

        exchange.getMessage().setHeader(FILE_NAME, filenameWithUUID);
        exchange.getMessage().setHeader(OBJECT_NAME, filenameWithUUID);
    }

    private String createFilenameWithUUID(String originalFileName) {
        String fileExtension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";
        String fileBeforeExtension = fileExtension.isEmpty() ? originalFileName : originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        return fileBeforeExtension + "." + UUID.randomUUID() + fileExtension + ".logline";
    }
}

