package org.memmcol.portalonboardservice.service.portal_user;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.portalonboardservice.components.GenericHandler;
import org.memmcol.portalonboardservice.mapper.PortalUserMapper;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.model.user.Organization;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.memmcol.portalonboardservice.components.handleValidUser.handleUserValidation;

@Service
public class PortalUserServiceImpl implements PortalUserService {

    private static final Logger log = LoggerFactory.getLogger(PortalUserServiceImpl.class);

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired private RestTemplate restTemplate;

    private final IMap<String, Operator> portalAuthCache;

    private final IMap<String, String> portalOtpCache;

    private final IMap<String, Boolean> portalVerifiedUsers;

    private final IMap<String, Boolean> portalOtpExpCache;

    private final Random random = new SecureRandom();

    public PortalUserServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.portalAuthCache = hazelcastInstance.getMap("portalAuthCache");
        this.portalOtpCache = hazelcastInstance.getMap("portalOtpCache");
        this.portalVerifiedUsers = hazelcastInstance.getMap("portalVerifiedUsers");
        this.portalOtpExpCache = hazelcastInstance.getMap("portalOtpExpCache");
    }

    @Transactional
    @Override
    public Map<String, Object> logout() {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);

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


            AuditLog auditLog = buildAuditLog(operator, "Logged out", "auth", null, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Logged out successfully", "");

        } catch (Exception exception) {
            log.error("Error occurred while logout: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "logging out operator");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> createOperator(Operator operator) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            Operator operatorAction = handleUserValidation();

            String email = operator.getEmail();

            Operator existingOperator = portalUserMapper.findByEmail(email);
            if(existingOperator != null) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Operator already exists with email ("+email+").");
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
            Operator operator1 = portalUserMapper.findByEmail(email);

            AuditLog auditLog = buildAuditLog(operatorAction, "Operator newly created", "operator", operator1, metadata);
            auditRepository.save(auditLog);

            portalAuthCache.put(operator1.getId().toString(), operator1);

//            handleAddCache(operator1);
            return ResponseMap.response(status.getSuccessCode(),
                    "Operator "+status.getRegDesc(),
                    ""
            );

        } catch (Exception exception){
            log.error("Error occurred while creating operator: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "creating operator");

            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateOperator(Operator operator) {

        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            Operator currentUser = handleUserValidation();

            UUID id =  operator.getId();
            Operator user = portalUserMapper.getSinglePortalUser(id);
            if(user == null){
                throw new GlobalExceptionHandler.NotFoundException("Operator does not exist");
            }
            Operator existingOperator = portalUserMapper.findByEmail(operator.getEmail());
            if(existingOperator.getEmail().equalsIgnoreCase(operator.getEmail())) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Operator already exists with email ("+operator.getEmail()+").");
            }

            if (!currentUser.getRoles().get(0).getUserRole().equalsIgnoreCase("SUPER_ADMIN")
                    && currentUser.getId() != operator.getId()
            ) {
                throw new GlobalExceptionHandler.ResourceAlreadyExistsException("Operator do not have permission to perform this action");
            }

            portalUserMapper.updatePortalUser(operator);

            String userRole = operator.getRole();
            if (userRole != null && (currentUser.getRoles().get(0).getUserRole().equalsIgnoreCase("SUPER_ADMIN")
                    || currentUser.getRoles().get(0).getUserRole().equalsIgnoreCase("ADMIN"))) {
                portalUserMapper.updateRole(userRole, id);
            }
            Operator operator1 = portalUserMapper.findByEmail(currentUser.getEmail());
            AuditLog auditLog = buildAuditLog(currentUser, "Edited operator", "operator", operator1, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), "Operator "+status.getUpdateDesc(), "");
        } catch (Exception exception){

            log.error("Error occurred while updating operator: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "editing operator");

            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> blockOperator(UUID id,boolean stat) {

        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            Operator user = handleUserValidation();

            if(id == user.getId()) {
                throw new GlobalExceptionHandler.NotFoundException("You cannot activate or deactivate yourself");
            }

            Operator result = portalUserMapper.getSinglePortalUser(id);
            if (result == null) {
               throw new GlobalExceptionHandler.NotFoundException("Operator not found");
            }
            portalUserMapper.blockAndUnblockOperator(id, stat);

            Operator operator = portalUserMapper.getSinglePortalUser(id);

            String desc = operator.isStatus() ? "Operator activated" : "Operator deactivated";

            AuditLog auditLog = buildAuditLog(user, desc, "operator", operator, metadata);
            auditRepository.save(auditLog);

            return ResponseMap.response(status.getSuccessCode(), desc+" successfully", "");

        }catch (Exception exception){
            log.error("Error occurred while blocking operator: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "changing state operator");

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getSingle() {

        try {
            Operator operatorAction = handleUserValidation();
            UUID currentOperator = operatorAction.getId();

            Operator result = portalUserMapper.getSinglePortalUser(currentOperator);

            return ResponseMap.response(status.getSuccessCode(),
                    status.getDesc(),
                    result
            );

        }catch (Exception exception){
            log.error("Error occurred while fetching operator: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "fetching operator");

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAll(String search,String role, Boolean state, int page, int size) {
        try {

            List<Operator> operators = portalUserMapper.getAllPortalUser();

            // Apply filtering by role and state
            List<Operator> filteredUsers = operators.stream()
                    .filter(o -> {
                        // Filter by role
                        if (role != null && !role.isEmpty()) {
                            String userRole = o.getRoles().get(0).getUserRole(); // Assuming first role is the main role
                            if (!userRole.equalsIgnoreCase(role)) {
                                return false;
                            }
                        }

                        // Filter by name (case-insensitive)
                        if (search != null && !search.isEmpty()) {
                            String searchLower = search.toLowerCase();
                            String fullName = o.getFirstname() + " " + o.getLastname();
                            String email = o.getEmail();

                            boolean matchesName = fullName.contains(searchLower);
                            boolean matchesEmail = email.contains(searchLower);

                            if (!matchesName && !matchesEmail) {
                                return false;
                            }
                        }

                        // Filter by state (assuming o.isState() returns boolean)
                        if (state != null && o.isStatus() != state) {
                            return false;
                        }
                        return true;
                    })
                    .toList();

            int totalFilteredUsers = filteredUsers.size();

            if (size <= 0) {
                size = totalFilteredUsers == 0 ? 1 : totalFilteredUsers;
            }
            if (page < 0) {
                page = 0;
            }

            int totalPages = (int) Math.ceil((double) totalFilteredUsers / size);
            int fromIndex = Math.min(page * size, totalFilteredUsers);
            int toIndex = Math.min(fromIndex + size, totalFilteredUsers);

            List<Operator> pagedUsers = filteredUsers.subList(fromIndex, toIndex);

            long  portalInActiveAdmin = operators.stream().filter(o -> !o.isActive()).count();
            long portalActiveAdmin = operators.stream().filter(o -> o.isActive()).count();
            long portalSuspendedAdmin = operators.stream().filter(o -> !o.isStatus()).count();


            Map<String, Object> result = new HashMap<>();
            result.put("totalPortalUsers", totalFilteredUsers);
            result.put("totalActiveAdmins", portalActiveAdmin);
            result.put("totalSuspendedAdmins", portalSuspendedAdmin);
            result.put("totalInActiveAdmins", portalInActiveAdmin);
            result.put("operators", pagedUsers); // return filtered list
            result.put("currentPage", page);
            result.put("totalPages", totalPages);
            result.put("pageSize", size);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Operators fetched successfully",
                    result
            );
        } catch (Exception exception) {
            log.error("Error occurred while fetching operator: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "fetching operator");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getRecentActivity(){
        try {

            Operator operatorAction = handleUserValidation();
            UUID currentOperator = operatorAction.getId();

            List<AuditLog> result = auditRepository.findTop5ByCreator_IdOrderByCreatedAtDesc(currentOperator);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Operators recent activities fetched successfully",
                    result
            );
        } catch (Exception exception) {
            log.error("Error occurred while fetching operator recent activities : {}", exception.getMessage(), exception);

            genericHandler.logAndSaveException(exception, "fetching activity");

            throw exception;
        }
    }

    @Override
    public Map<String, Object> changePassword(String username, String oldPassword, String newPassword) {
        try {
            Operator user = handleUserValidation();

            // Validate username and old password
            if (!username.trim().equalsIgnoreCase(user.getEmail())) {
                throw new GlobalExceptionHandler.NotFoundException("Invalid username");
            }

            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new GlobalExceptionHandler.NotFoundException("Old password provided is incorrect");
            }

            // Encode and update new password
            String newEncodedPassword = passwordEncoder.encode(newPassword);
            int changePwd = portalUserMapper.changePassword(user.getEmail(), newEncodedPassword, user.getId());

            if (changePwd == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Change password failed");
            }

            return ResponseMap.response(status.getSuccessCode(), "Password changed successfully", "");
        } catch (Exception exception) {
            log.error("Error occurred while [changing password]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "changing password");
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
    public  Map<String, Object> verifyOtp(String email, String otp, String password) {
        try {

            Operator isOperator = portalUserMapper.findByAuthEmail(email);
            if (isOperator == null) {
                throw new RuntimeException("User not found");
            }
            String storedOtp = portalOtpCache.get(email);

            if (storedOtp != null && storedOtp.equals(otp)) {
                portalOtpCache.remove(email);

                portalVerifiedUsers.put(email, true, 2, TimeUnit.MINUTES);

                return handleForgetPassword(isOperator, password);

            }
            throw new GlobalExceptionHandler.NotFoundException("OTP verification failed");

        } catch (Exception exception){
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "verifying otp");
            throw exception;
        }
    }


    private Map<String, Object>  handleGenerateOtp(String username) {
        String otp = String.format("%04d", random.nextInt(10000));

        String emailServiceUrl = "http://localhost:8084/api/send";

        try {
            restTemplate.postForEntity(emailServiceUrl, Map.of(
                    "toAddress", username,
                    "subject", "OTP Code",
                    "message", otp
            ), Void.class);
        } catch (RestClientException emailException) {
            log.error("Failed to send OTP email to {}: {}", username, emailException.getMessage().trim(), emailException);
            genericHandler.logAndSaveException(emailException, "sending email");
            throw emailException;
        }

        portalOtpCache.put(username, otp);
        return ResponseMap.response(status.getSuccessCode(), "OTP Generated and sent successfully", "");
    }


    public Map<String, Object> handleForgetPassword(Operator user, String password) {
        Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
        try {

            if (!portalVerifiedUsers.containsKey(user.getEmail())) {
                return ResponseMap.response(status.getNotFoundCode(), "OTP verification required before password change", "");
            }

            int passwordChangeResult = portalUserMapper.resetPassword(user.getEmail(), passwordEncoder.encode(password));
            if (passwordChangeResult == 0) {
                return ResponseMap.response(status.getBlockCode(),  "Operator " + status.getBlockFailureDesc(), "");
            }

            user.setPassword("");
            // Remove OTP verification from cache after successful password reset
            portalVerifiedUsers.remove(user.getEmail());
//			handleCacheUpdate(isOperator);
            AuditLog auditLog = buildAuditLog(user, "Reset password", "auth", null, metadata);
            auditRepository.save(auditLog);
            return ResponseMap.response(status.getSuccessCode(), "Password " + status.getUpdateDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "changing password");
            throw exception;
        }
    }


    private void blacklistToken(String token, int expirySeconds) {
        portalOtpExpCache.put(token, true, expirySeconds, TimeUnit.SECONDS);
    }
    private AuditLog buildAuditLog(Operator creator, String description, String type, Object createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setOperator(createdEntity instanceof Operator ? (Operator) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndPoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }
}


