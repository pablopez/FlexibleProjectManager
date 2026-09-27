package com.flexibleprojectmanager.platform.authentication.application;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("Authentication failed.");
    }
}
