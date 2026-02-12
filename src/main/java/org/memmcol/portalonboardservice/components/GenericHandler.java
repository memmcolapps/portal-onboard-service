package org.memmcol.portalonboardservice.components;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GenericHandler {

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public void logAndSaveException(Exception exception, String actionDescription) {

        ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
        exceptionErrorLogs.setDescription("Error occurred while trying to " + actionDescription);
        exceptionErrorLogs.setError_message(exception.getMessage().trim());
        exceptionErrorLogs.setError(exception.toString().trim());

        exceptionAuditRepository.save(exceptionErrorLogs);
    }

    public Map<String, String> extractRequestMetadata(HttpServletRequest request) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ipAddress", getClientIp(request));
        metadata.put("userAgent", request.getHeader("User-Agent"));
        metadata.put("endpoint", request.getRequestURI());
        metadata.put("httpMethod", request.getMethod());
        return metadata;
    }

    public static String getClientIp(HttpServletRequest request) {
        /*
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
        */

        // Debug logging for AWS API Gateway
        log.debug("IP Headers Debug - X-Forwarded-For: {}, X-Real-IP: {}, RemoteAddr: {}",
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getRemoteAddr());

        // Check X-Forwarded-For (standard header, may contain: client-ip, proxy1, proxy2, ...)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // Take the first IP (original client) before any commas
            String[] ips = ip.split(",");
            for (String potentialIp : ips) {
                String trimmedIp = potentialIp.trim();
                if (!"unknown".equalsIgnoreCase(trimmedIp)) {
                    log.debug("X-Forwarded-For found multiple IPs, using first valid: {}", trimmedIp);
                    return trimmedIp;
                }
            }
        }

        // Check X-Real-IP (used by Nginx, Cloudflare, and some proxies)
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            log.debug("Using X-Real-IP: {}", ip);
            return ip;
        }

        // Fallback to direct connection IP
        String remoteAddr = request.getRemoteAddr();
        log.debug("Using RemoteAddr (fallback): {}", remoteAddr);
        return remoteAddr;
    }

}
