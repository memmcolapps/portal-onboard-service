package org.memmcol.portalonboardservice.service.auditlog;

import org.memmcol.portalonboardservice.components.GenericHandler;
import org.memmcol.portalonboardservice.config.ResponseProperties;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.AuditLogDto;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.model.user.Role;
import org.memmcol.portalonboardservice.model.user.UserModel;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.memmcol.portalonboardservice.components.handleValidUser.handleUserValidation;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditRepository auditRepository;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private GenericHandler genericHandler;

    public AuditLogServiceImpl(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public Map<String, Object> getAuditLog(int page, int size) {
        try {
            Operator um = handleUserValidation();
            Map<String, Object> response = new HashMap<>();

            // If page or size is null, 0, or less than 1, fetch all
            if (page < 0 || size <= 0) {
                List<AuditLogDto> result = auditRepository.findAllByCreator_Id(um.getId())
                        .stream()
                        .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                        .map(log -> new AuditLogDto(
                                log.getId(),
                                log.getType(),
                                log.getCreator().getFirstname() + " " + log.getCreator().getLastname(),
                                log.getCreator().getEmail(),
                                log.getCreator().getRoles().get(0).getUserRole(),
                                log.getDescription(),
                                log.getUserAgent(),
                                log.getIpAddress(),
                                log.getCreatedAt(),
                                log.getReason(),
                                log.getEndPoint(),
                                log.getHttpMethod()
                        ))
                        .collect(Collectors.toList());
                response.put("data", result);
                response.put("size", result.size());

            } else {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<AuditLog> pagedResult = auditRepository.findAllByCreator_IdOrderByCreatedAtDesc(um.getId(),pageable);

                List<AuditLogDto> result = pagedResult.getContent().stream()
                        .map(log -> new AuditLogDto(
                                log.getId(),
                                log.getType(),
                                log.getCreator().getFirstname() + " " + log.getCreator().getLastname(),
                                log.getCreator().getEmail(),
                                log.getCreator().getRoles().get(0).getUserRole(),
                                log.getDescription(),
                                log.getUserAgent(),
                                log.getIpAddress(),
                                log.getCreatedAt(),
                                log.getReason(),
                                log.getEndPoint(),
                                log.getHttpMethod()

                        ))
                        .collect(Collectors.toList());
                response.put("data", result);
                response.put("page", pagedResult.getNumber());
                response.put("totalData", pagedResult.getTotalElements());
                response.put("size", pagedResult.getTotalPages());
            }
            return ResponseMap.response(status.getSuccessCode(), "Logs " + status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error occurred while fetching audit logs: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "fetching audit");

            throw exception;
        }
    }

}
