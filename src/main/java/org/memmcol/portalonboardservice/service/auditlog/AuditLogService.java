package org.memmcol.portalonboardservice.service.auditlog;

import java.util.Map;

public interface AuditLogService {
    Map<String, Object> getAuditLog(String role, String name, String email, int page, int size );
}
