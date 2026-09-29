package com.flexibleprojectmanager.platform.licensing.infrastructure;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.licensing")
public class LicenseProperties {
    private Map<String, String> trustedKeys = new LinkedHashMap<>();

    public Map<String, String> getTrustedKeys() {
        return trustedKeys;
    }

    public void setTrustedKeys(Map<String, String> trustedKeys) {
        this.trustedKeys = trustedKeys == null ? new LinkedHashMap<>() : new LinkedHashMap<>(trustedKeys);
    }
}
