package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogStreamException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter.NativeLogStreamSplitterProcessor.LOG_PACKET_EXTENSION;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter.NativeLogStreamSplitterProcessor.LOG_PACKET_MAX_SIZE;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
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
        when(message.getHeader(FILE_NAME, String.class)).thenReturn(null);

        assertThrows(InvalidLogStreamException.class, () -> nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(exchange));
    }

    @Test
    void filenameWithExtensionGetsUuidAndLoglineSuffix() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn("sikkerhets-test.20260210.auditlog");

        nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(exchange);

        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> objectNameCaptor = ArgumentCaptor.forClass(String.class);

        verify(message, times(1)).setHeader(org.mockito.ArgumentMatchers.eq(FILE_NAME), fileNameCaptor.capture());
        verify(message, times(1)).setHeader(org.mockito.ArgumentMatchers.eq(OBJECT_NAME), objectNameCaptor.capture());

        String generatedFileName = fileNameCaptor.getValue();
        assertEquals(generatedFileName, objectNameCaptor.getValue());
        assertTrue(generatedFileName.startsWith("sikkerhets-test.20260210."));
        assertTrue(generatedFileName.endsWith(LOG_PACKET_EXTENSION));
    }

    @Test
    void filenameWithoutExtensionStillGetsLoglineSuffix() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getHeader(FILE_NAME, String.class)).thenReturn("sikkerhets-test");

        nativeLogStreamSplitterProcessor.prepareLogPacketHeaders(exchange);

        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(message, times(1)).setHeader(org.mockito.ArgumentMatchers.eq(FILE_NAME), fileNameCaptor.capture());

        String generatedFileName = fileNameCaptor.getValue();
        assertTrue(generatedFileName.startsWith("sikkerhets-test."));
        assertTrue(generatedFileName.endsWith(LOG_PACKET_EXTENSION));
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

