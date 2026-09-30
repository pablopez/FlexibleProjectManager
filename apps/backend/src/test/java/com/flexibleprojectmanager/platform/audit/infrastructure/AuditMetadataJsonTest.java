package com.flexibleprojectmanager.platform.audit.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.flexibleprojectmanager.platform.audit.domain.AuditMetadata;

class AuditMetadataJsonTest {
    @Test void roundTripsEscapedAndCommaContainingFieldNames() {
        AuditMetadata metadata = new AuditMetadata(List.of("field,with,commas", "quoted\"field", "slash\\field"));
        assertEquals(metadata.changedFields(), AuditMetadataJson.read(AuditMetadataJson.write(metadata)));
    }

    @Test void malformedOrExtendedMetadataIsIgnoredSafely() {
        assertEquals(null, AuditMetadataJson.read("{\"changedFields\":[\"unterminated]}"));
        assertEquals(null, AuditMetadataJson.read("{\"changedFields\":[],\"extra\":true}"));
    }
}
