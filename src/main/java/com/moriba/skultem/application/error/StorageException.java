package com.moriba.skultem.application.error;

public class StorageException extends RuntimeException {

    private final String details;

    public StorageException(String message, String details, Throwable cause) {
        super(message, cause);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
