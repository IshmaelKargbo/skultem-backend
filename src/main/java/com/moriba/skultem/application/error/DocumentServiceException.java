package com.moriba.skultem.application.error;

// The document service (M-Compose) could not make a file: it is not set up, cannot be reached, or refused the
// request. The message is safe to show to the user; it never contains a key.
public class DocumentServiceException extends RuntimeException {

    public DocumentServiceException(String message) {
        super(message);
    }

    public DocumentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
