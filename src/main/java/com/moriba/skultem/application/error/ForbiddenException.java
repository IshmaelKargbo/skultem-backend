package com.moriba.skultem.application.error;

// Signed in, but not allowed to see or do this (e.g. another family's child). Answered with 403, not 401
// like AccessDeniedException: the frontend treats 401 as an expired session and logs the user out.
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
