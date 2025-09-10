package org.memmcol.portalonboardservice.service.auditlog;

import java.util.Map;

public interface AuditLogService {
    Map<String, Object> getAuditLog(int page, int size);
}
