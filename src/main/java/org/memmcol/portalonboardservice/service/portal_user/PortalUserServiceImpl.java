package org.memmcol.portalonboardservice.service.portal_user;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.portalonboardservice.mapper.PortalUserMapper;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.model.user.Role;
import org.memmcol.portalonboardservice.model.user.UserModel;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.memmcol.portalonboardservice.util.ResponseProperties;
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
import java.util.stream.Collectors;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired private RestTemplate restTemplate;

    private final IMap<String, Object> authCache;

    private final IMap<String, String> otpCache;

    private final IMap<String, Boolean> verifiedUsers;

    private final Random random = new SecureRandom();

    public PortalUserServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.authCache = hazelcastInstance.getMap("authCache");
        this.otpCache = hazelcastInstance.getMap("otpCache");
        this.verifiedUsers = hazelcastInstance.getMap("verifiedUsers");
    }

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

    @Override
    public Map<String, Object> createOperator(Operator operator) {

        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

        try {

            String email = operator.getEmail();

            Optional<Operator> existingOperator = portalUserMapper.findByEmail(email);
            if (existingOperator.isPresent()) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Operator already exists");
            }

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
            Operator operatorAction = handleUserValidation();

            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            auditNotificationDTO.setCreator(operatorAction);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setDescription("Created new operator account "+ operator.getEmail());
            auditNotificationDTO.setType("auth");
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),
                    "Created new operator successfully",
                    ""
            );

        }catch (Exception exception){
            log.error("Error occurred while creating operator: {}", exception.getMessage(), exception);
            errorLog.setDescription("Error occurred while creating operator");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }


    @Override
    public Map<String, Object> updateOperator(Operator operator) {

        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

        try {

            Operator currentUser = handleUserValidation();

            UUID id =  operator.getId();
            Optional<Operator> user = portalUserMapper.getSinglePortalUser(id);
            if (user.isPresent()) {
                portalUserMapper.updatePortalUser(operator);

                String userRole = operator.getRole();
                if (userRole != null) {
                    portalUserMapper.updateRole(userRole, id);
                }

            }else {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Operator does not exist");
            }

            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            auditNotificationDTO.setCreator(currentUser);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setDescription("Updated operator account with ID: " + operator.getId());
            auditNotificationDTO.setType("auth");
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Operator updated successfully",
                    ""
            );
        }catch (Exception exception){

            log.error("Error occurred while updating operator: {}", exception.getMessage(), exception);
            errorLog.setDescription("Error occurred while updating operator");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Override
    public Map<String, Object> blockOperator(UUID id,boolean stat) {

        AuditLog auditNotificationDTO = new AuditLog();
        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

        try {

            Optional<Operator> result = portalUserMapper.getSinglePortalUser(id);
            if (result.isPresent()) {
                portalUserMapper.blockAndUnblockOperator(id, stat);
            }

            boolean isStatus = portalUserMapper.getSinglePortalUser(id)
                    .map(Operator::isStatus)
                    .orElse(false);

            Operator operatorAction = handleUserValidation();

            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");

            auditNotificationDTO.setCreator(operatorAction);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setType("auth");

            if (!isStatus) {

                auditNotificationDTO.setDescription("Blocked operator account with ID: " + id);
                auditRepository.save(auditNotificationDTO);

                return ResponseMap.response(status.getSuccessCode(),
                        "Operator Suspended successfully",
                        ""
                );
            } else {
                auditNotificationDTO.setDescription("Activated operator account with ID: " + id);
                auditRepository.save(auditNotificationDTO);
                return ResponseMap.response(status.getSuccessCode(),
                        "Operator Activated successfully",
                        ""
                );
            }

//            return ResponseMap.response(status.getSuccessCode(),
//                    "Operator Suspended successfully",
//                    ""
//            );

        }catch (Exception exception){
            log.error("Error occurred while blocking operator: {}", exception.getMessage(), exception);
            errorLog.setDescription("Error occurred while blocking operator");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Override
    public Map<String, Object> getSingle(UUID id) {

        try {
            Optional<Operator> result = portalUserMapper.getSinglePortalUser(id);
            if (result.isEmpty()) {
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

    @Override
    public Map<String, Object> getAll() {

        try {
            List<Operator> operators = portalUserMapper.getAllPortalUser();

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Operators fetched successfully",
                    operators
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

    @Override
    public Map<String, Object> generateOtp(String username) {
        return handleGenerateOtp(username);
    }

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
        authCache.put(token, true, expirySeconds, TimeUnit.SECONDS);
    }
}
