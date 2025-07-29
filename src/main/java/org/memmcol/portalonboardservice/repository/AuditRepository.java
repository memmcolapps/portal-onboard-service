package org.memmcol.portalonboardservice.repository;

import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditRepository extends MongoRepository<AuditLog, String> {
}