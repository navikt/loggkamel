package no.nav.sikkerhetstjenesten.loggkamel.service;

public class ColdStorageProxyException extends RuntimeException {
    public ColdStorageProxyException(Throwable cause) {
        super("DB2 proxy request failed during cold-storage export", cause);
    }
}
