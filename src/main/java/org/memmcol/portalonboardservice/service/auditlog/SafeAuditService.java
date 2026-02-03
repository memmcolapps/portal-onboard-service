package org.memmcol.portalonboardservice.service.auditlog;

import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SafeAuditService {

    private final AuditRepository auditRepository;

    public SafeAuditService(@Autowired(required = false) AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void saveAudit(AuditLog auditLog) {
        if (auditRepository == null) {
            // Mongo repository not available
            return;
        }

        try {
            auditRepository.save(auditLog);
        } catch (Exception e) {
            // Log the error but don't throw
            System.err.println("Mongo save failed: " + e.getMessage());
            // optionally use a logger:
            // log.warn("Failed to save audit log", e);
        }
    }
}

