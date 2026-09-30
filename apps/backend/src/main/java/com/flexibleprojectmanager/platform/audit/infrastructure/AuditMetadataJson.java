package com.flexibleprojectmanager.platform.audit.infrastructure;

import java.util.List;

import com.flexibleprojectmanager.platform.audit.domain.AuditMetadata;

final class AuditMetadataJson {
    private AuditMetadataJson() {}
    static String write(AuditMetadata metadata) {
        if (metadata == null) return null;
        return "{\"changedFields\":[" + metadata.changedFields().stream().map(AuditMetadataJson::quote).collect(java.util.stream.Collectors.joining(",")) + "]}";
    }
    static List<String> read(String json) {
        if (json == null) return null;
        try {
            String prefix = "{\"changedFields\":[";
            if (!json.startsWith(prefix) || !json.endsWith("]}")) return null;
            String body = json.substring(prefix.length(), json.length() - 2);
            List<String> values = parseStrings(body);
            return new AuditMetadata(values).changedFields();
        } catch (Exception ignored) { return null; }
    }
    private static List<String> parseStrings(String body) {
        List<String> values = new java.util.ArrayList<>();
        int index = 0;
        while (index < body.length()) {
            if (body.charAt(index++) != '"') throw new IllegalArgumentException();
            StringBuilder value = new StringBuilder();
            boolean closed = false;
            while (index < body.length()) {
                char character = body.charAt(index++);
                if (character == '"') { closed = true; break; }
                if (character != '\\') { value.append(character); continue; }
                if (index >= body.length()) throw new IllegalArgumentException();
                char escaped = body.charAt(index++);
                switch (escaped) {
                    case '"' -> value.append('"'); case '\\' -> value.append('\\'); case '/' -> value.append('/');
                    case 'b' -> value.append('\b'); case 'f' -> value.append('\f'); case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r'); case 't' -> value.append('\t');
                    case 'u' -> { if (index + 4 > body.length()) throw new IllegalArgumentException(); value.append((char) Integer.parseInt(body.substring(index, index + 4), 16)); index += 4; }
                    default -> throw new IllegalArgumentException();
                }
            }
            if (!closed) throw new IllegalArgumentException();
            values.add(value.toString());
            if (index == body.length()) break;
            if (body.charAt(index++) != ',') throw new IllegalArgumentException();
        }
        return values;
    }
    private static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
}
