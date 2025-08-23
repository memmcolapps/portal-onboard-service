package org.memmcol.portalonboardservice.service.portal_user;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.portalonboardservice.mapper.PortalUserMapper;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.model.user.Role;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.memmcol.portalonboardservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.memmcol.portalonboardservice.util.GenericHandler.getClientIp;
import static org.memmcol.portalonboardservice.components.handleValidUser.handleUserValidation;

@Service
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired private RestTemplate restTemplate;

    private final IMap<String, Operator> authCache;

    private final IMap<String, String> otpCache;

    private final IMap<String, Boolean> verifiedUsers;

    private final IMap<String, Boolean> portalOtpExpCache;

    private final Random random = new SecureRandom();

    public PortalUserServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.authCache = hazelcastInstance.getMap("portalAuthCache");
        this.otpCache = hazelcastInstance.getMap("portalOtpCache");
        this.verifiedUsers = hazelcastInstance.getMap("portalVerifiedUsers");
        this.portalOtpExpCache = hazelcastInstance.getMap("portalOtpExpCache");
    }

    @Transactional
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

    @Transactional
    @Override
    public Map<String, Object> createOperator(Operator operator) {

        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            Operator operatorAction = handleUserValidation();

            String email = operator.getEmail();

            Operator existingOperator = portalUserMapper.findByEmail(email);

            boolean user = authCache.containsKey(existingOperator.getId().toString());
            if(user){
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Operator already exists");
            }
//
//            if (existingOperator != null) {
//                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Operator already exists");
//            }

            String password = passwordEncoder.encode(operator.getPassword());
            operator.setPassword(password);
            operator.setStatus(true);
            operator.setActive(true);

            portalUserMapper.createPortalUser(operator);

            Role role = new Role();
            role.setUserId(operator.getId());
            role.setUserRole(operator.getRole());

            boolean result;
            result = portalUserMapper.createPortalRole(role);
            if (result == false){
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Failed to create Role");
            }
            Operator operator1 = portalUserMapper.findByEmail(email);
            auditNotificationDTO.setCreator(operatorAction);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setDescription("Operator newly created");
            auditNotificationDTO.setType("operator");
            auditNotificationDTO.setOperator(operator1);
            auditRepository.save(auditNotificationDTO);

            authCache.put(operator1.getId().toString(), operator1);

//            handleAddCache(operator1);
            return ResponseMap.response(status.getSuccessCode(),
                    "Operator "+status.getRegDesc(),
                    ""
            );

        } catch (Exception exception){
            log.error("Error occurred while creating operator: {}", exception.getMessage(), exception);
            errorLog.setDescription("Error occurred while creating operator");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateOperator(Operator operator) {

        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            Operator currentUser = handleUserValidation();

            UUID id =  operator.getId();
            Operator user = portalUserMapper.getSinglePortalUser(id);
            if(user == null){
                throw new GlobalExceptionHandler.NotFoundException("Operator does not exist");
            }

            if (!currentUser.getRoles().get(0).getUserRole().equalsIgnoreCase("SUPER_ADMIN")
                    && currentUser.getId() != operator.getId()
            ) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Operator do not have permission to perform this action");
            }

            portalUserMapper.updatePortalUser(operator);

            String userRole = operator.getRole();
            if (userRole != null && (currentUser.getRoles().get(0).getUserRole().equalsIgnoreCase("SUPER_ADMIN")
                    || currentUser.getRoles().get(0).getUserRole().equalsIgnoreCase("WRITE"))) {
                portalUserMapper.updateRole(userRole, id);
            }
            Operator operator1 = portalUserMapper.findByEmail(currentUser.getEmail());

            auditNotificationDTO.setCreator(currentUser);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setDescription("Operator updated");
            auditNotificationDTO.setType("operator");
            auditNotificationDTO.setOperator(operator1);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), "Operator "+status.getUpdateDesc(), "");
        } catch (Exception exception){

            log.error("Error occurred while updating operator: {}", exception.getMessage(), exception);
            errorLog.setDescription("Error occurred while updating operator");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> blockOperator(UUID id,boolean stat) {

        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            Operator operatorAction = handleUserValidation();

            Operator result = portalUserMapper.getSinglePortalUser(id);
            if (result == null) {
               throw new GlobalExceptionHandler.NotFoundException("Operator not found");
            }
            portalUserMapper.blockAndUnblockOperator(id, stat);

            Operator operator = portalUserMapper.getSinglePortalUser(id);

            String desc = operator.isStatus() ? "Operator activated" : "Operator deactivated";

            auditNotificationDTO.setCreator(operatorAction);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType("operator");
            auditNotificationDTO.setOperator(operator);
            auditNotificationDTO.setDescription(desc);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(), desc+" successfully", "");

        }catch (Exception exception){
            log.error("Error occurred while blocking operator: {}", exception.getMessage(), exception);
            errorLog.setDescription("Error occurred while blocking operator");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getSingle(UUID id) {

        try {
            Operator result = portalUserMapper.getSinglePortalUser(id);
            if (result == null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Operator not found");
            }

            return ResponseMap.response(status.getSuccessCode(),
                    status.getDesc(),
                    result
            );

        }catch (Exception exception){
            log.error("Error occurred while fetching operator: {}", exception.getMessage(), exception);
            ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

            errorLog.setDescription("Error occurred while fetching operator");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAll() {

        try {
            List<Operator> operators = portalUserMapper.getAllPortalUser();

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Operators fetched successfully",
                    operators
            );
        } catch (Exception exception){

            log.error("Error occurred while fetching operator: {}", exception.getMessage(), exception);
            ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

            errorLog.setDescription("Error occurred while fetching operator");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> generateOtp(String username) {
        return handleGenerateOtp(username);
    }

    @Transactional
    @Override
    public  Map<String, Object> verifyOtp(String email, String otp, String password, String retypePassword) {
        try {

            if(!password.equals(retypePassword)){
                return ResponseMap.response(status.getNotFoundCode(), "Passwords do not match", "");
            }
            Operator isOperator = portalUserMapper.findByAuthEmail(email);
            if (isOperator == null) {
                throw new RuntimeException("User not found");
            }
            String storedOtp = otpCache.get(email);
            if (storedOtp != null && storedOtp.equals(otp)) {
                otpCache.remove(email);

                verifiedUsers.put(email, true, 2, TimeUnit.MINUTES);

                return handleForgetPassword(isOperator, password);

            }
            throw new GlobalExceptionHandler.NotFoundException("OTP verification failed");

        } catch (Exception exception){
            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while verifying OTP");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }


    private Map<String, Object>  handleGenerateOtp(String username) {
        String otp = String.format("%04d", random.nextInt(10000));

        String emailServiceUrl = "http://localhost:8081/smarte/email/api/send";

        try {
            restTemplate.postForEntity(emailServiceUrl, Map.of(
                    "toAddress", username,
                    "subject", "OTP Code",
                    "message", otp
            ), Void.class);
        } catch (RestClientException emailException) {
            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
            log.error("Failed to send OTP email to {}: {}", username, emailException.getMessage().trim(), emailException);
            exceptionErrorLogs.setDescription("Error occurred while generating OTP");
            exceptionErrorLogs.setError_message(emailException.getMessage().trim());
            exceptionErrorLogs.setError(emailException.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw emailException;
        }

        otpCache.put(username, otp);
        return ResponseMap.response(status.getSuccessCode(), "OTP Generated and sent successfully", "");
    }


    public Map<String, Object> handleForgetPassword(Operator user, String password) {
        AuditLog AuditLog = new AuditLog();
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        try {

            if (!verifiedUsers.containsKey(user.getEmail())) {
                return ResponseMap.response(status.getNotFoundCode(), "OTP verification required before password change", "");
            }

            int passwordChangeResult = portalUserMapper.resetPassword(user.getEmail(), passwordEncoder.encode(password));
            if (passwordChangeResult == 0) {
                return ResponseMap.response(status.getBlockCode(),  "Operator " + status.getBlockFailureDesc(), "");
            }
            user.setPassword("");
            // Remove OTP verification from cache after successful password reset
            verifiedUsers.remove(user.getEmail());
//			handleCacheUpdate(isOperator);
            AuditLog.setCreator(user);
            AuditLog.setUserAgent(userAgent);
            AuditLog.setIpAddress(ipAddress);
            AuditLog.setDescription("Reset password");
            AuditLog.setType("auth");
            auditRepository.save(AuditLog);
            return ResponseMap.response(status.getSuccessCode(), "Password " + status.getUpdateDesc(), "");

        } catch (Exception exception) {
            ExceptionErrorLogs exceptionErrorLogs = new ExceptionErrorLogs();
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            exceptionErrorLogs.setDescription("Error occurred while changing operator password");
            exceptionErrorLogs.setError_message(exception.getMessage().trim());
            exceptionErrorLogs.setError(exception.toString().trim());
            exceptionAuditRepository.save(exceptionErrorLogs);
            throw exception;
        }
    }


    private void blacklistToken(String token, int expirySeconds) {
        portalOtpExpCache.put(token, true, expirySeconds, TimeUnit.SECONDS);
    }

//    private void handleAddCache(Operator operator) {
////        bandCache.remove(band.getId().toString()+"_"+band.getOrgId());
////        tariffCache.clear();
//        for (String key : auditCache.keySet()) {
//            if (key.startsWith("grid_flex_audit_log_page_")) {
//                auditCache.remove(key);
//            }
//        }
//        for (String key : bandCache.keySet()) {
//            if (key.startsWith("bands_"+band.getOrgId())) {
//                bandCache.remove(key);
//            }
//        }
//        bandCache.put(band.getId().toString()+"_"+band.getOrgId(), band);  // Cache updated or deleted entity
//    }
}
