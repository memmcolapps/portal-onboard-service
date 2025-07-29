package org.memmcol.portalonboardservice.repository;

import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExceptionAuditRepository extends MongoRepository<ExceptionErrorLogs, String> {
}
