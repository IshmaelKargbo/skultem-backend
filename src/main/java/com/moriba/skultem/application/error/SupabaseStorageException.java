package com.moriba.skultem.application.error;

public class SupabaseStorageException extends RuntimeException {

    private final String details;

    public SupabaseStorageException(String message, String details, Throwable cause) {
        super(message, cause);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}