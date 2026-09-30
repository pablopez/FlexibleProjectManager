package com.flexibleprojectmanager.platform.audit.application;

import com.flexibleprojectmanager.platform.audit.domain.AuditEvent;

public interface AuditRecorder {
    void record(AuditEvent event);
}
