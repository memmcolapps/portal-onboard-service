package org.memmcol.portalonboardservice.service.portal_user;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.portalonboardservice.mapper.PortalUserMapper;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.service.organization.OnboardOrganizationServiceImpl;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.memmcol.portalonboardservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.memmcol.portalonboardservice.util.GenericHandler.getClientIp;
import static org.memmcol.portalonboardservice.util.handleValidUser.handleUserValidation;

@Service
@Transactional
public class PortalUserServiceImpl implements PortalUserService {

    private static final Logger log = LoggerFactory.getLogger(PortalUserServiceImpl.class);

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private PortalUserMapper portalUserMapper;

    private final IMap<String, Object> authCache;

    public PortalUserServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.authCache = hazelcastInstance.getMap("authCache");
    }

//    @Override
//    public Map<String, Object> logout(String token, int expirySeconds) {
//        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
//        AuditLog auditNotificationDTO = new AuditLog();
//        try {
//            String ipAddress = getClientIp(httpServletRequest);
//            String userAgent = httpServletRequest.getHeader("User-Agent");
//            Operator operator = handleUserValidation();
//            operator.setPassword("");
//            portalUserMapper.updateLogoutState(operator.getEmail());
//            blacklistToken(token, expirySeconds);
//            auditNotificationDTO.setCreator(operator);
//            auditNotificationDTO.setUserAgent(userAgent);
//            auditNotificationDTO.setIpAddress(ipAddress);
//            auditNotificationDTO.setDescription("Logged out");
//            auditNotificationDTO.setType("auth");
////            removeFromCache();
////			authCache.remove("dashboard");
//            auditRepository.save(auditNotificationDTO);
//            return ResponseMap.response(status.getSuccessCode(), "Logged out successfully", "");
//        } catch (Exception exception) {
//            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
//            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
//            exceptionErrorLogs.setDescription("Error occurred while logout");
//            exceptionErrorLogs.setError_message(exception.getMessage().trim());
//            exceptionErrorLogs.setError(exception.toString().trim());
//            exceptionAuditRepository.save(exceptionErrorLogs);
//            throw exception;
//        }
//
//    }

    @Override
    public Map<String, Object> logout() {
        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            // Extract raw token without decoding
            String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Authorization header is missing or malformed");
            }
            String rawToken = authorizationHeader.substring("Bearer ".length());

            Operator operator = handleUserValidation();
            operator.setPassword("");
            portalUserMapper.updateLogoutState(operator.getEmail());

            // Blacklist the raw token
            blacklistToken(rawToken, 1800);

            auditNotificationDTO.setCreator(operator);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setDescription("Logged out");
            auditNotificationDTO.setType("auth");
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), "Logged out successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while logout: {}", exception.getMessage(), exception);
            errorLog.setDescription("Error occurred while logout");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);
            throw exception;
        }
    }

    private void blacklistToken(String token, int expirySeconds) {
        System.out.println(">>>>>>>>token:: "+token);
        authCache.put(token, true, expirySeconds, TimeUnit.SECONDS);
    }

    @Override
    public Map<String, Object> getAll() {
        return Map.of();
    }
}
