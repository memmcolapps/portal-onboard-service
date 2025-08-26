package org.memmcol.portalonboardservice.service.organization;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.portalonboardservice.mapper.OrganizationMapper;
import org.memmcol.portalonboardservice.mapper.UserMapper;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.node.Node;
import org.memmcol.portalonboardservice.model.user.*;
import org.memmcol.portalonboardservice.model.user.Module;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.memmcol.portalonboardservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;

import static org.memmcol.portalonboardservice.util.GenericHandler.getClientIp;
import static org.memmcol.portalonboardservice.components.handleValidUser.handleUserValidation;


@Service
public class OnboardOrganizationServiceImpl implements OnboardOrganizationService {


    private final OrganizationMapper organizationMapper;
    private final ExceptionAuditRepository exceptionAuditRepository;
    private static final Logger log = LoggerFactory.getLogger(OnboardOrganizationServiceImpl.class);

    @Autowired
    private ResponseProperties status;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private PasswordEncoder passwordEncoder;

//    @Autowired
//    private CacheManager cacheManager;

    private final IMap<String, Organization> organizationCache;

    // Other mappers can be added as needed
    public OnboardOrganizationServiceImpl(OrganizationMapper organizationMapper,
                                          ExceptionAuditRepository exceptionAuditRepository,
                                          @Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.organizationMapper = organizationMapper;
        this.exceptionAuditRepository = exceptionAuditRepository;
        this.organizationCache = hazelcastInstance.getMap("organizationCache");
    }

    @Transactional
    @Override
    public Map<String, Object> addOrganization(Organization organization, UserModel userModel) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            Operator operator = handleUserValidation();

            // Save to database
            organizationMapper.insertOrganization(organization);
            UUID orgId = organization.getId();
            String name = organization.getBusinessName();

            // Create root node
            Node rootNode = new Node();
            rootNode.setName(name);
            rootNode.setOrgId(orgId);

            organizationMapper.insertNodes(rootNode);
            UUID rootNodeId = rootNode.getId();

            Permission permission = new Permission();

            permission.setView(true);
            permission.setEdit(true);
            permission.setApprove(true);
            permission.setDisable(true);
            permission.setOrgId(orgId);

            int result;
            result = organizationMapper.insertPermission(permission);
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException("Fail to create permission");

            Group group = new Group();

            group.setGroupTitle("User management");
            group.setOrgId(orgId);

            result = organizationMapper.insertGroup(group);
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException("Fail to create group");

            Module module = new Module();
            module.setAccess(true);
            module.setName("User management");
            module.setGroupId(group.getId());
            module.setOrgId(orgId);

            result = organizationMapper.insertModule(module);
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException("Fail to create module");

            SubModule subModule = new SubModule();
            subModule.setAccess(true);
            subModule.setName("User management");
            subModule.setModuleId(module.getId());
            subModule.setOrgId(orgId);
            result = organizationMapper.insertSubModule(subModule);
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException("Fail to create submodule");

            result = organizationMapper.insertGroupPermission(group.getId(), permission.getId(), orgId);
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException("Fail to create group permission");

            userModel.setOrgId(orgId);
            userModel.setNodeId(rootNodeId);
            userModel.setStatus(true);
            userModel.setActive(false);
            userModel.setPassword(passwordEncoder.encode(userModel.getPassword()));
            result = organizationMapper.insertUser(userModel);
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException("Fail to create user");
//            UUID id = userModel.getId();

            result = organizationMapper.insertUserGroup(userModel.getId(), orgId, group.getId());
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException("Fail to create user group");

            organizationMapper.updateOrg(userModel.getId(), orgId);
            if(result == 0) throw new GlobalExceptionHandler.NotFoundException("Fail to create group permission");


            Organization res = organizationMapper.getOrganizationById(organization.getId());

            // Save into Hazelcast cache (persists via MapStore)
//            organizationCache.put(res.getId().toString(), res);
            auditNotificationDTO.setCreator(operator);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setDescription("Organization created");
            auditNotificationDTO.setType("organization");
            auditNotificationDTO.setOrganization(res);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    (organization.getBusinessName() + " Organization Created " + " Successfully"),
                    "");

        } catch (Exception exception) {
            log.error("Error creating organization: {}", exception.getMessage(), exception);

            // Log exception to audit system
            errorLog.setDescription("Error creating organization");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);
            throw exception;

        }
    }

    // Permissions, groups, user creation
//            if (createDefaultPermission(orgId) == 0)
//                throw new GlobalExceptionHandler.NotFoundException("Fail to create permission");
//            if (createDefaultGroup(orgId) == 0)
//                throw new GlobalExceptionHandler.NotFoundException("Fail to create group");
//            if (createDefaultUserGroup(orgId) == 0)
//                throw new GlobalExceptionHandler.NotFoundException("Fail to create user group");
//            if (createDefaultGroupPermission(orgId) == 0)
//                throw new GlobalExceptionHandler.NotFoundException("Fail to create group permission");
//            if (createDefaultUser(orgId, rootNodeId, userModel) == 0)
//                throw new GlobalExceptionHandler.NotFoundException("Fail to create user");

//    @Transactional
//    public int createDefaultUser(UUID organizationId, UUID nodeId, UserModel userModel) {
//
//            userModel.setOrgId(organizationId);
//            userModel.setNodeId(nodeId);
//            userModel.setStatus(true);
//            userModel.setActive(false);
//            userModel.setPassword(passwordEncoder.encode(userModel.getPassword()));
//            int result;
//            result = organizationMapper.insertUser(userModel);
//            UUID id = userModel.getId();
//            organizationMapper.updateOrg(id, organizationId);
//            return result;
//
//    }

//    @Transactional
//    public int createDefaultPermission(UUID organizationId) {
//            Permission permission = new Permission();
//
//            permission.setView(true);
//            permission.setEdit(true);
//            permission.setApprove(true);
//            permission.setDisable(true);
//            permission.setOrgId(organizationId);
//
//            int result;
//            result = organizationMapper.insertPermission(permission);
//            return result;
//    }

//    @Transactional
//    public int createDefaultGroup(UUID organizationId) {
//            Group group = new Group();
//
//            group.setGroupTitle("User management");
//            group.setOrgId(organizationId);
//
//            int result;
//            result = organizationMapper.insertGroup(group);
//            return result;
//    }

//    @Transactional
//    public int createDefaultGroupPermission(UUID organizationId) {
//        System.out.println("oorId: "+organizationId);
//            Permission permission = organizationMapper.getPermissionByOrgId(organizationId);
//            Group group = organizationMapper.getGroupByOrgId(organizationId);
//
//            int response;
//            response = organizationMapper.insertGroupPermission(group.getId(), permission.getId(), organizationId);
//            return response;
//    }

    @Transactional(readOnly = true)
//    @Cacheable(value = "organizationCache", key = "'allOrgs'")
    @Override
    public Map<String, Object> getOrganization() {
        try {

            List<Organization> organizations = organizationMapper.getAllOrganizations();
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

            int totalOrganizations = organizations.size();
            int totalActiveOrganizations = 0;
            BigDecimal totalVending = BigDecimal.ZERO;
            BigDecimal totalBilling = BigDecimal.ZERO;
            Long overallCustomers = 0L;
            Long overallFeeders = 0L;
            BigDecimal overallVending = BigDecimal.ZERO;
            BigDecimal overallBilling = BigDecimal.ZERO;

            for (Organization org : organizations) {
                if (org.getStatus()) {
                    totalActiveOrganizations++;
                }

                // Build node tree...
                List<Node> nodes = organizationMapper.getAllNode(org.getId());

                Map<UUID, Node> nodeMap = new HashMap<>();
                Node root = null;

                for (Node node : nodes) {
                    node.setNodesTree(new ArrayList<>());
                    nodeMap.put(node.getId(), node);
                }

                for (Node node : nodes) {
                    if (node.getId().equals(org.getOperator().getNodeId())) {
                        root = node; // this is the node we're querying for
                    }
                    if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
                        Node parent = nodeMap.get(node.getParentId());
                        parent.getNodesTree().add(node);
                    }
                }
                org.setNodes(root);
                Long orgCustomerCount = organizationMapper.totalCustomer(org.getId());
                Long orgFeederCount = organizationMapper.totalFeeder(org.getId());
                BigDecimal orgVendingTotal = BigDecimal.valueOf(0);
                BigDecimal orgBillingTotal = BigDecimal.valueOf(0);

                // Set in organization object
                org.setTotalCustomer(orgCustomerCount);
                org.setTotalFeeder(orgFeederCount);
                org.setTotalVending(orgVendingTotal != null ? orgVendingTotal : BigDecimal.ZERO);
                org.setTotalBilling(orgBillingTotal != null ? orgBillingTotal : BigDecimal.ZERO);

                // Accumulate for global totals
                overallCustomers += orgCustomerCount;
                overallFeeders += orgFeederCount;
                overallVending = totalVending.add(org.getTotalVending());
                overallBilling = totalBilling.add(org.getTotalBilling());

                if (org.getImage() != null) {
                    // Convert relative path to full URL
                    String fullUrl = baseUrl + org.getImage();
                    org.setImage(fullUrl);
                }
            }


            Map<String, Object> response = new HashMap<>();
            response.put("totalOrganizations", totalOrganizations);
            response.put("totalActiveOrganizations", totalActiveOrganizations);
            response.put("overallCustomer", overallCustomers);
            response.put("overallVending", overallVending);
            response.put("overallBilling", overallBilling);
            response.put("overallFeeder", overallFeeders);
            response.put("organizations", organizations);
//            organizationCache.put("allOrgs", response);
            return ResponseMap.response(status.getSuccessCode(), "Organizations "+status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error fetching organizations {}", exception.getMessage(), exception);

            ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
            errorLog.setDescription("Error fetching organizations");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
//    @Cacheable(value = "organizationCache", key = "#orgId")
    @Override
    public Map<String, Object> getOrganizationById(UUID orgId) {
//        Map<String, Object> res;
        try {
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            BigDecimal orgVendingTotal = BigDecimal.valueOf(0);
            BigDecimal orgBillingTotal = BigDecimal.valueOf(0);

//            // 1. Check cache first
//            Organization res = organizationCache.get(orgId.toString());
//            if (res != null) {
//                log.info("Cache hit for Organization {}", orgId);
//                return ResponseMap.response(status.getSuccessCode(), status.getDesc(), res);
//            }

            Organization result = organizationMapper.getOrganizationById(orgId);

            if (result == null) {
                throw  new GlobalExceptionHandler.NotFoundException("Organization not found");
            }

            Long totalCustomer = organizationMapper.totalCustomer(result.getId());

            Long totalFeeder = organizationMapper.totalFeeder(result.getId());

            List<Node> nodes = organizationMapper.getAllNode(orgId);

            Map<UUID, Node> nodeMap = new HashMap<>();
            Node root = null;

            for (Node node : nodes) {
                node.setNodesTree(new ArrayList<>());
                nodeMap.put(node.getId(), node);
            }

            for (Node node : nodes) {
                if (node.getId().equals(result.getOperator().getNodeId())) {
                    root = node; // this is the node we're querying for
                }
                if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
                    Node parent = nodeMap.get(node.getParentId());
                    parent.getNodesTree().add(node);
                }
            }

            if (result.getImage() != null) {
                // Convert relative path to full URL
                String fullUrl = baseUrl + result.getImage();
                result.setImage(fullUrl);
            }
            result.setNodes(root);

            result.setTotalCustomer(totalCustomer);
            result.setTotalFeeder(totalFeeder);
            result.setTotalVending(orgVendingTotal != null ? orgVendingTotal : BigDecimal.ZERO);
            result.setTotalBilling(orgBillingTotal != null ? orgBillingTotal : BigDecimal.ZERO);

//            organizationCache.put(result.getId().toString(), result);

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), result);


        } catch (Exception exception) {
            log.error("Error fetching organization {}", exception.getMessage(), exception);

            ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
            errorLog.setDescription("Error fetching organization");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;

        }

    }

//    @CacheEvict(value = "organizationCache", key = "'allOrgs'")
    @Transactional
    @Override
    public Map<String, Object> updateOrganization(Organization organization,UserModel userModel) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            Operator operator = handleUserValidation();

            Organization res = organizationMapper.getOrganizationById(organization.getId());

            if (res == null) {
                throw  new GlobalExceptionHandler.NotFoundException("Organization not found");
            }

            int result;
            result = organizationMapper.updateOrganizationSelective(organization);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Fail to update organization");
            }
            result = organizationMapper.updateUserByOrgId(userModel, organization.getId());
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Fail to update organization");
            }
//
//            // Update the allOrgs cache manually
//            Cache cache = cacheManager.getCache("organizationCache");
//            if (cache != null) {
//                Map<String, Object> cachedAllOrgs = cache.get("allOrgs", Map.class);
//                if (cachedAllOrgs != null) {
//                    List<Organization> orgs = (List<Organization>) cachedAllOrgs.get("organizations");
//                    if (orgs != null) {
//                        orgs.replaceAll(o -> o.getId().equals(organization.getId()) ? organization : o);
//                        cachedAllOrgs.put("organizations", orgs);
//                        cache.put("allOrgs", cachedAllOrgs);
//                    }
//                }
//            }

            auditNotificationDTO.setCreator(operator);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setDescription("Organization edited");
            auditNotificationDTO.setType("organization");
            auditNotificationDTO.setOrganization(res);
            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(status.getSuccessCode(),
                    "Organization "+status.getUpdateDesc(),
                    "");

        } catch (Exception exception) {
            log.error("Error updating organization: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error updating organization");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);
            throw exception;
        }

    }

    @Override
    public Map<String, Object> suspendOrganization(UUID id, Boolean suspend) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        AuditLog auditNotificationDTO = new AuditLog();
        try {
            String ipAddress = getClientIp(httpServletRequest);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            Operator operator = handleUserValidation();
            Organization res = organizationMapper.getOrganizationById(id);

            if (res == null) {
                throw  new GlobalExceptionHandler.NotFoundException("Organization not found");
            }

            organizationMapper.suspendOrganization(id, suspend);

            Organization response = organizationMapper.getOrganizationById(id);

            String desc = response.getStatus() ? "Organization activated" : "Organization suspended";

            auditNotificationDTO.setCreator(operator);
            auditNotificationDTO.setIpAddress(ipAddress);
            auditNotificationDTO.setUserAgent(userAgent);
            auditNotificationDTO.setDescription(desc);
            auditNotificationDTO.setType("organization");
            auditNotificationDTO.setOrganization(res);
            auditRepository.save(auditNotificationDTO);
            return ResponseMap.response(status.getSuccessCode(), desc + " successfully", "");

        } catch (Exception exception) {
            log.error("Error updating organization: {}", exception.getMessage(), exception);
            errorLog.setDescription("Error updating organization");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);
            throw exception;
        }
    }

    private void addChangeIfDifferent(String fieldName, String oldValue, String newValue,
                                      Map<String, Map<String, String>> changes) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.put(fieldName, Map.of(
                    "old", oldValue != null ? oldValue : "null",
                    "new", newValue != null ? newValue : "null"
            ));
        }
    }
}
