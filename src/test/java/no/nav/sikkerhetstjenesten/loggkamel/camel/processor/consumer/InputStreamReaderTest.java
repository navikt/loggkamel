package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogGroupException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class InputStreamReaderTest {

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @InjectMocks
    InputStreamReader inputStreamReader;

    @Test
    void prepareBodyAsInputStream_doesNothingWhenBodyIsAlreadyInputStream() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody()).thenReturn(mock(InputStream.class));

        inputStreamReader.prepareBodyAsInputStream(exchange);

        verifyNoMoreInteractions(exchange, message);
    }

    @Test
    void prepareBodyAsInputStream_makesInputStreamFromBlob() {
        when(exchange.getMessage()).thenReturn(message);
        Blob blob = mock(Blob.class);
        when(message.getBody()).thenReturn(blob);
        ReadChannel readChannel = mock(ReadChannel.class);
        when(blob.reader()).thenReturn(readChannel);

        inputStreamReader.prepareBodyAsInputStream(exchange);

        verify(message).setBody(any(InputStream.class));
        verifyNoMoreInteractions(exchange, message);
    }

    @Test
    void prepareBodyAsInputStream_exceptionIfBodyNotCoercibleToInputStream() {
        when(exchange.getIn()).thenReturn(message);
        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody()).thenReturn("This is not an InputStream or Blob");
        when(message.getBody(InputStream.class)).thenReturn(null);

        assertThrows(InvalidPostgresLogGroupException.class, () -> inputStreamReader.prepareBodyAsInputStream(exchange));
    }

    @Test
    void prepareBodyAsInputStream_coercesBodyToInputStream() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody()).thenReturn("This is not an InputStream or Blob");
        InputStream stream = mock(InputStream.class);
        when(message.getBody(InputStream.class)).thenReturn(stream);

        inputStreamReader.prepareBodyAsInputStream(exchange);

        verify(message).setBody(stream);
        verifyNoMoreInteractions(exchange, message);
    }

}