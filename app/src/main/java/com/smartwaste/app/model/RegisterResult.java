package com.smartwaste.app.model;

public class RegisterResult {
    private final boolean success;
    private final String errorMessage;

    public RegisterResult(boolean success, String errorMessage) {
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
