package org.memmcol.portalonboardservice.repository;

import org.memmcol.portalonboardservice.model.DataAuditDTO;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditRepository extends MongoRepository<DataAuditDTO, String> {
}