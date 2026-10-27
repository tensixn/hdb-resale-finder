package com.abcde.hdbresale.gateway;

/** An external API failed, timed out, or returned something unusable, and no cached value could stand in. */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
