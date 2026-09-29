package com.flexibleprojectmanager.platform.licensing.application;

public class LicenseException extends RuntimeException {
    private final String code;

    public LicenseException(String code) {
        super(code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
