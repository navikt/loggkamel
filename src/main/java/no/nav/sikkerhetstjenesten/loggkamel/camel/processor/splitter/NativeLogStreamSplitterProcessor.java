package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Service
public class NativeLogStreamSplitterProcessor {

    private static final Logger log = LoggerFactory.getLogger(NativeLogStreamSplitterProcessor.class);
    static final int LOG_PACKET_MAX_SIZE = 1000;

    static final String LOG_PACKET_EXTENSION = ".packet";

    public void prepareLogPacketHeaders(Exchange exchange) {
        String logStreamFilename = exchange.getMessage().getHeader(FILE_NAME, String.class);

        if (logStreamFilename == null || logStreamFilename.isEmpty()) {
            log.warn("Filename header is missing while splitting log stream");
            throw new InvalidLogStreamException("Filename header is missing while splitting log stream");
        }

        String logPacketFilename = createFilenameWithUUID(logStreamFilename);
        log.debug("New filename being assigned to packet: {}", logPacketFilename);

        exchange.getMessage().setHeader(FILE_NAME, logPacketFilename);
        exchange.getMessage().setHeader(OBJECT_NAME, logPacketFilename);
    }

    private String createFilenameWithUUID(String originalFileName) {
        String fileExtension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";
        String fileBeforeExtension = fileExtension.isEmpty() ? originalFileName : originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        return fileBeforeExtension + "." + UUID.randomUUID() + fileExtension + LOG_PACKET_EXTENSION;
    }

    public Iterator<List<String>> groupIntoPackets(Exchange exchange) {
        InputStream logGroupInputStream = exchange.getMessage().getBody(InputStream.class);

        if (logGroupInputStream == null) {
            throw new InvalidLogStreamException("Input stream is missing while splitting log group");
        }

        return new LogPacketIterator(logGroupInputStream, LOG_PACKET_MAX_SIZE);
    }

    /**
     * Reads the log stream lazily and yields packets with at most packetSize entries.
     * Each entry mirrors the old tokenize("^\\<|\n\\<") semantics.
     */
    static final class LogPacketIterator implements Iterator<List<String>> {

        private final BufferedReader reader;
        private final int packetSize;
        private boolean exhausted;
        private String nextLogEntry;
        private String bufferedStartOfNextEntry;

        LogPacketIterator(InputStream inputStream, int packetSize) {
            this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            this.packetSize = packetSize;
        }

        @Override
        public boolean hasNext() {
            if (exhausted) {
                return false;
            }

            if (nextLogEntry != null) {
                return true;
            }

            nextLogEntry = readNextLogEntry();
            return nextLogEntry != null;
        }

        @Override
        public List<String> next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more log packets available");
            }

            List<String> packet = new ArrayList<>(packetSize);
            packet.add(nextLogEntry);
            nextLogEntry = null;

            while (packet.size() < packetSize) {
                String logEntry = readNextLogEntry();
                if (logEntry == null) {
                    break;
                }
                packet.add(logEntry);
            }

            return packet;
        }

        private String readNextLogEntry() {
            if (exhausted) {
                return null;
            }

            try {
                String line;
                StringBuilder current = bufferedStartOfNextEntry == null
                        ? null
                        : new StringBuilder(bufferedStartOfNextEntry);
                bufferedStartOfNextEntry = null;

                // Reads line by line. Lines starting with "<" are the start of a new log entry. Lines not starting with "<"
                // are a continuation of the current log entry.
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("<")) {
                        String logEntryStart = line.substring(1);
                        if (current != null) {
                            bufferedStartOfNextEntry = logEntryStart;
                            return current.toString();
                        }
                        current = new StringBuilder(logEntryStart);
                    } else if (current == null) {
                        current = new StringBuilder(line);
                    } else {
                        current.append('\n').append(line);
                    }
                }

                exhausted = true;
                closeReader();
                return current == null ? null : current.toString();
            } catch (IOException e) {
                exhausted = true;
                closeReader();
                throw new UncheckedIOException("Failed while splitting log stream", e);
            }
        }

        private void closeReader() {
            try {
                reader.close();
            } catch (IOException e) {
                log.warn("Failed to close log stream reader", e);
            }
        }
    }
}

