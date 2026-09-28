package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_PACKET_INDEX;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter.NativeLogStreamSplitterProcessor.LOG_PACKET_MAX_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NativeLogStreamSplitterProcessorTest {

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @InjectMocks
    NativeLogStreamSplitterProcessor nativeLogStreamSplitterProcessor;

    @Test
    void missingFileNameThrows() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(LOG_FILENAME, String.class)).thenReturn(null);

        assertThrows(InvalidLogStreamException.class, () -> nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(exchange));
    }

    @Test
    void filenameWithExtensionGetsSequentialNumberAndPacketSuffix() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(LOG_FILENAME, String.class)).thenReturn("sikkerhets-test.20260210.auditlog");
        when(message.getHeader(LOG_PACKET_INDEX, Integer.class)).thenReturn(0, 1);

        nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(exchange);
        nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(exchange);

        verify(message).setHeader(LOG_FILENAME, "sikkerhets-test.20260210.1.auditlog.packet");
        verify(message).setHeader(LOG_FILENAME, "sikkerhets-test.20260210.2.auditlog.packet");
    }

    @Test
    void filenameWithoutExtensionGetsSequentialNumberAndPacketSuffix() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(LOG_FILENAME, String.class)).thenReturn("sikkerhets-test");
        when(message.getHeader(LOG_PACKET_INDEX, Integer.class)).thenReturn(0);

        nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(exchange);

        verify(message).setHeader(LOG_FILENAME, "sikkerhets-test.1.packet");
    }

    @Test
    void missingPacketIndexThrows() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(LOG_FILENAME, String.class)).thenReturn("sikkerhets-test");

        assertThrows(InvalidLogStreamException.class, () -> nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(exchange));
    }

    @Test
    void packetSequenceRestartsForEachStreamWithoutChangingEntries() {
        List<String> entries = List.of("first\ncontinuation", "second");
        DefaultCamelContext context = new DefaultCamelContext();
        Exchange firstStream = new DefaultExchange(context);
        firstStream.getMessage().setHeader(LOG_FILENAME, "first.auditlog");
        firstStream.getMessage().setHeader(LOG_PACKET_INDEX, 0);
        firstStream.getMessage().setBody(entries);
        Exchange secondStream = new DefaultExchange(context);
        secondStream.getMessage().setHeader(LOG_FILENAME, "second.auditlog");
        secondStream.getMessage().setHeader(LOG_PACKET_INDEX, 0);
        secondStream.getMessage().setBody(entries);

        nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(firstStream);
        nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(secondStream);

        assertEquals("first.1.auditlog.packet", firstStream.getMessage().getHeader(LOG_FILENAME));
        assertEquals("second.1.auditlog.packet", secondStream.getMessage().getHeader(LOG_FILENAME));
        assertEquals(entries, firstStream.getMessage().getBody());
        assertEquals(entries, secondStream.getMessage().getBody());
    }

    @Test
    void groupIntoPacketsDemarcatedByNewPacketCharacter() {
        String input = "<first\ncontinuation\n<second\n<third";

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(java.io.InputStream.class))
                .thenReturn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        Iterator<List<String>> packets = nativeLogStreamSplitterProcessor.groupIntoPackets(exchange);

        assertTrue(packets.hasNext());
        assertEquals(List.of("first\ncontinuation", "second", "third"), packets.next());
        assertFalse(packets.hasNext());
    }

    @Test
    void groupIntoPacketsSplitsIntoThousandSizedPackets() {
        StringBuilder inputBuilder = new StringBuilder();
        for (int i = 0; i < LOG_PACKET_MAX_SIZE + 1; i++) {
            inputBuilder.append("<entry-").append(i).append("\n");
        }

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(java.io.InputStream.class))
                .thenReturn(new ByteArrayInputStream(inputBuilder.toString().getBytes(StandardCharsets.UTF_8)));

        Iterator<List<String>> packets = nativeLogStreamSplitterProcessor.groupIntoPackets(exchange);

        List<String> firstPacket = packets.next();
        List<String> secondPacket = packets.next();

        assertEquals(LOG_PACKET_MAX_SIZE, firstPacket.size());
        assertEquals("entry-0", firstPacket.getFirst());
        assertEquals("entry-999", firstPacket.getLast());

        assertEquals(1, secondPacket.size());
        assertEquals("entry-1000", secondPacket.getFirst());
        assertFalse(packets.hasNext());
    }
}
