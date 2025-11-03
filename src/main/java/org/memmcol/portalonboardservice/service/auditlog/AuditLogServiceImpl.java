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
    public Map<String, Object> getAuditLog(String role, String name, String email, int page, int size) {
        try {
            Operator um = handleUserValidation();
            Map<String, Object> response = new HashMap<>();

            // Fetch all logs by creator
            List<AuditLog> logs = auditRepository.findAllByCreator_Id(um.getId());

            // Apply filtering
            List<AuditLog> filteredLogs = logs.stream()
                    .filter(log -> {
                        // Filter by role
                        if (role != null && !role.isEmpty()) {
                            String userRole = log.getCreator().getRoles().get(0).getUserRole();
                            if (!userRole.equalsIgnoreCase(role)) {
                                return false;
                            }
                        }

                        // Filter by name (first + last)
                        if (name != null && !name.isEmpty()) {
                            String fullName = (log.getCreator().getFirstname() + " " + log.getCreator().getLastname()).toLowerCase();
                            if (!fullName.contains(name.toLowerCase())) {
                                return false;
                            }
                        }

                        // Filter by email
                        if (email != null && !email.isEmpty()) {
                            if (!log.getCreator().getEmail().toLowerCase().contains(email.toLowerCase())) {
                                return false;
                            }
                        }

                        return true;
                    })
                    .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                    .collect(Collectors.toList());

            // ===== Pagination =====
            int totalFiltered = filteredLogs.size();
            if (size <= 0) size = totalFiltered == 0 ? 1 : totalFiltered;
            if (page < 0) page = 0;

            int fromIndex = Math.min(page * size, totalFiltered);
            int toIndex = Math.min(fromIndex + size, totalFiltered);
            List<AuditLog> pagedLogs = filteredLogs.subList(fromIndex, toIndex);

            // ===== Map to DTO =====
            List<AuditLogDto> result = pagedLogs.stream()
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

            // ===== Response =====
            response.put("data", result);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", (int) Math.ceil((double) totalFiltered / size));
            response.put("totalFilteredLogs", totalFiltered);

            return ResponseMap.response(status.getSuccessCode(), "Logs " + status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error occurred while fetching audit logs: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "fetching audit");
            throw exception;
        }
    }


}
