package com.moriba.skultem.application.error;

/** The caller is limited to management sections that don't cover what they asked for. */
public class OutsideManagementScopeException extends RuntimeException {

    public OutsideManagementScopeException(String message) {
        super(message);
    }

    public OutsideManagementScopeException() {
        this("This is outside the management sections you have access to.");
    }
}
