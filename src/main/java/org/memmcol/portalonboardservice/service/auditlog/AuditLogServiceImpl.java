package org.memmcol.portalonboardservice.service.auditlog;

import org.memmcol.portalonboardservice.components.GenericHandler;
import org.memmcol.portalonboardservice.config.ResponseProperties;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.AuditLogDto;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private final AuditRepository auditRepository;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private GenericHandler genericHandler;

    public AuditLogServiceImpl(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public Map<String, Object> getAuditLog(String role, String search, int page, int size) {
        try {
            Operator um = handleUserValidation();
            Map<String, Object> response = new HashMap<>();
            List<AuditLog> logs;
            try{
                 logs = auditRepository.findAllByCreator_Id(um.getId());
            }catch (Exception e) {
                // Log the error but don't throw
                System.err.println("Mongo save failed: " + e.getMessage());
                // log.warn("Failed to save audit log", e);
            }
            // Fetch all logs by creator
            logs = auditRepository.findAllByCreator_Id(um.getId());

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

                        // Combined search (name OR email)
                        if (search != null && !search.isEmpty()) {
                            String searchLower = search.toLowerCase();
                            String fullName = (log.getCreator().getFirstname() + " " + log.getCreator().getLastname()).toLowerCase();
                            String email = log.getCreator().getEmail().toLowerCase();

                            boolean matchesName = fullName.contains(searchLower);
                            boolean matchesEmail = email.contains(searchLower);

                            if (!matchesName && !matchesEmail) {
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