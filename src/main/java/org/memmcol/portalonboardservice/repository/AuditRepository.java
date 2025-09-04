package org.memmcol.portalonboardservice.repository;

import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface AuditRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findTop5ByCreator_IdOrderByCreatedAtDesc(UUID orgId);
}