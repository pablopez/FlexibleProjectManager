package com.flexibleprojectmanager.platform.audit.application;

import com.flexibleprojectmanager.platform.audit.domain.AuditEvent;

public interface AuditEventRepository {
    void append(AuditEvent event);
}
