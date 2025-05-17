package com.masakapa.app.model;

public class LoginResult {
    private final boolean success;
    private final String errorMessage;

    public LoginResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
