package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error;

import com.google.cloud.storage.StorageException;
import org.apache.camel.Exchange;
import org.apache.camel.spi.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Custom ExceptionHandler for exceptions thrown during the GCS consumer's polling phase (i.e., before
 * an exchange enters the Camel route). These exceptions are not reachable by the route's {@code onException}
 * handlers; a dedicated {@code ExceptionHandler} bean wired into the consumer endpoint URI is required.
 *
 * <p>The most common expected exception in a competing-consumers setup with {@code deleteAfterRead=true}
 * is a {@link StorageException} with HTTP 404: another instance has already consumed and deleted the object
 * by the time this consumer tries to download its body. This is treated as a normal operating condition and
 * logged at DEBUG level rather than WARN.
 *
 * <p>All other exceptions are logged at WARN with full details so genuinely unexpected polling errors
 * remain visible.
 *
 * <p>Reference this bean in any GCS consumer endpoint URI with:
 * {@code &exceptionHandler=#gcpConsumerPollingExceptionHandler}
 */
@Component("gcpConsumerPollingExceptionHandler")
public class GcpConsumerPollingExceptionHandler implements ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GcpConsumerPollingExceptionHandler.class);
    private static final int HTTP_NOT_FOUND = 404;

    @Override
    public void handleException(Throwable exception) {
        handleException("Exception without message or exchange information provided", null, exception);
    }

    @Override
    public void handleException(String message, Throwable exception) {
        handleException(message, null, exception);
    }

    @Override
    public void handleException(String message, Exchange exchange, Throwable exception) {
        //TODO: remove after testing
        log.error("Captured exception during GCS consumer polling: " + exception);

        StorageException storageException = findStorageException(exception);

        //TODO: remove after testing
        log.error("Captured storage exception: " + storageException);
        if (storageException != null && storageException.getCode() == HTTP_NOT_FOUND) {
            log.debug(
                "Ignoring 404 Not Found during GCS consumer polling — object was likely already " +
                "consumed and deleted by a competing instance. This is expected behaviour. " +
                "Component message: {}",
                message
            );
        } else {
            log.warn(
                "Unexpected exception during GCS consumer polling: {}",
                message,
                exception
            );
        }
    }

    /**
     * Walks the exception cause chain looking for a {@link StorageException}.
     * The Camel component sometimes wraps the original exception in a runtime wrapper.
     */
    private StorageException findStorageException(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof StorageException storageException) {
                return storageException;
            }
            current = current.getCause();
        }
        return null;
    }
}

