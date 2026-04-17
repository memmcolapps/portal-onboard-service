package org.memmcol.portalonboardservice.service.organization;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.portalonboardservice.components.GenericHandler;
import org.memmcol.portalonboardservice.mapper.NodeMapper;
import org.memmcol.portalonboardservice.mapper.OrganizationMapper;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.node.Node;
import org.memmcol.portalonboardservice.model.node.RegionBhubServiceCenter;
import org.memmcol.portalonboardservice.model.node.SubStationTransformerFeederLine;
import org.memmcol.portalonboardservice.model.user.*;
import org.memmcol.portalonboardservice.model.user.Module;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.service.auditlog.SafeAuditService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.memmcol.portalonboardservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;

import static org.memmcol.portalonboardservice.components.handleValidUser.handleUserValidation;


@Service
public class OnboardOrganizationServiceImpl implements OnboardOrganizationService {


private final OrganizationMapper organizationMapper;
    private final NodeMapper nodeMapper;
    private final ExceptionAuditRepository exceptionAuditRepository;
    private static final Logger log = LoggerFactory.getLogger(OnboardOrganizationServiceImpl.class);

    @Autowired
    private ResponseProperties status;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private PasswordEncoder passwordEncoder;

//    @Autowired
//    private CacheManager cacheManager;

    @Autowired
    private GenericHandler genericHandler;

    private final IMap<String, Organization> organizationCache;

    // Other mappers can be added as needed
public OnboardOrganizationServiceImpl(OrganizationMapper organizationMapper,
                                          NodeMapper nodeMapper,
                                          ExceptionAuditRepository exceptionAuditRepository,
                                          @Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.organizationMapper = organizationMapper;
        this.nodeMapper = nodeMapper;
        this.exceptionAuditRepository = exceptionAuditRepository;
        this.organizationCache = hazelcastInstance.getMap("organizationCache");
    }

    @Transactional
    @Override
    public Map<String, Object> addOrganization(Organization organization, UserModel userModel) {

        try {
            handleRequired(organization, userModel);

            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            Operator operator = handleUserValidation();

            UserModel email = organizationMapper.getUserByEmail(userModel.getEmail());
            if (email != null) {
                throw new GlobalExceptionHandler.NotFoundException("Email already used");
            }

            // Save to database ---
            organizationMapper.insertOrganization(organization);
            UUID orgId = organization.getId();
            String name = organization.getBusinessName();

            // Create root node
            Node rootNode = new Node();
            rootNode.setName(name);
            rootNode.setOrgId(orgId);

            organizationMapper.insertNodes(rootNode);
            UUID rootNodeId = rootNode.getId();

            // Create root
            RegionBhubServiceCenter regionBhubServiceCenter = new RegionBhubServiceCenter();
            regionBhubServiceCenter.setOrgId(orgId);
            regionBhubServiceCenter.setNodeId(rootNodeId);
            regionBhubServiceCenter.setName(userModel.getFirstname());
            regionBhubServiceCenter.setContactPerson(userModel.getFirstname()+' '+userModel.getLastname());
            regionBhubServiceCenter.setEmail(userModel.getEmail());
            regionBhubServiceCenter.setAddress(organization.getAddress());
            regionBhubServiceCenter.setPhoneNo(userModel.getPhoneNumber());

            String regionId;

            do {
                regionId = String.valueOf(100000 + new Random().nextInt(900000));
            } while (Boolean.TRUE.equals(nodeMapper.existByRegionId(regionId)));

            regionBhubServiceCenter.setRegionId(regionId);
            regionBhubServiceCenter.setType("root");
            nodeMapper.createRegionBhubServiceCenter(regionBhubServiceCenter);

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
            AuditLog auditLog = buildAuditLog(operator, "Organization created", "organization", res, metadata);
//            auditRepository.save(auditLog);
            safeAuditService.saveAudit(auditLog);
//            auditNotificationDTO.setCreator(operator);
//            auditNotificationDTO.setIpAddress(ipAddress);
//            auditNotificationDTO.setUserAgent(userAgent);
//            auditNotificationDTO.setDescription("Organization created");
//            auditNotificationDTO.setType("organization");
//            auditNotificationDTO.setOrganization(res);
//            auditRepository.save(auditNotificationDTO);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Organization Created Successfully",
                    "");

        } catch (Exception exception) {
            log.error("Error creating organization: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "creating organization");
            throw exception;

        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException(fieldName + " is required");
        }
    }

    private void handleRequired(Organization orgRequest, UserModel userRequest) {

        validateRequired(orgRequest.getBusinessName(), "Business name");
        validateRequired(orgRequest.getAddress(), "Address");
        validateRequired(orgRequest.getCountry(), "Country");
        validateRequired(orgRequest.getState(), "State");
        validateRequired(orgRequest.getCity(), "City");

        validateRequired(userRequest.getFirstname(), "Firstname");
        validateRequired(userRequest.getLastname(), "Lastname");
        validateRequired(userRequest.getEmail(), "Email");
        validateRequired(userRequest.getPhoneNumber(), "Phone number");
        validateRequired(userRequest.getPassword(), "Password");
    }

    @Transactional(readOnly = true)
//    @Cacheable(value = "organizationCache", key = "'allOrgs'")
    @Override
    public Map<String, Object> getOrganization(String nameFilter,String statusFilter,int page, int size) {
        try {

            List<Organization> organizations = organizationMapper.getAllOrganizations();

            if (nameFilter != null && !nameFilter.trim().isEmpty() || statusFilter != null && !statusFilter.trim().isEmpty()) {


                String name = nameFilter != null ? nameFilter.trim().toLowerCase() : null;
                String stat = statusFilter != null ? statusFilter.trim().toLowerCase() : null;

                organizations = organizations.stream()
                        .filter(org -> {
                            boolean matchesName = ((name == null || name.isEmpty()) || (org.getBusinessName() != null && org.getBusinessName().toLowerCase().contains(name)));
                            boolean matchesStatus = ((stat == null || stat.isEmpty()))
                                    || (org.getStatus() != null &&
                                    ((stat.equals("active") && org.getStatus()) ||
                                            (stat.equals("suspended") && !org.getStatus())));
                            return matchesName && matchesStatus;
                        })
                        .toList();
            }
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

            int totalOrganizations = organizations.size();
            int totalActiveOrganizations = 0;
            BigDecimal totalVending = BigDecimal.ZERO;
            BigDecimal totalBilling = BigDecimal.ZERO;
            Long overallCustomers = 0L;
            Long overallFeeders = 0L;
            BigDecimal overallVending = BigDecimal.ZERO;
            BigDecimal overallBilling = BigDecimal.ZERO;

            if (size <= 0) {
                size = totalOrganizations == 0 ? 1 : totalOrganizations;
            }
            if (page < 0) {
                page = 0;
            }
            int totalPages = (int) Math.ceil((double) totalOrganizations / size);

            List<Organization> pagedOrganizations;
            if (organizations.isEmpty()) {
                pagedOrganizations = Collections.emptyList();
            } else {
                int fromIndex = Math.min(page * size, totalOrganizations);
                int toIndex = Math.min(fromIndex + size, totalOrganizations);
                pagedOrganizations = organizations.subList(fromIndex, toIndex);
            }

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
                BigDecimal orgVendingTotal = organizationMapper.totalVending(org.getId());
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
            response.put("organizations", pagedOrganizations);
            response.put("currentPage", page);
            response.put("totalPages", totalPages);
            response.put("pageSize", size);
//            organizationCache.put("allOrgs", response);
            return ResponseMap.response(status.getSuccessCode(), "Organizations "+status.getDesc(), response);

        } catch (Exception exception) {
            log.error("Error fetching organizations {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "fetching organization");
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
            BigDecimal orgVendingTotal = organizationMapper.initialSumVended(orgId);
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
            genericHandler.logAndSaveException(exception, "fetching organization");
            throw exception;

        }

    }

//    @CacheEvict(value = "organizationCache", key = "'allOrgs'")
    @Transactional
    @Override
    public Map<String, Object> updateOrganization(Organization organization,UserModel userModel) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            Operator operator = handleUserValidation();

            Organization res = organizationMapper.getOrganizationById(organization.getId());

            if (res == null) {
                throw  new GlobalExceptionHandler.NotFoundException("Organization not found");
            }

            if (userModel.getEmail() != null) {
                UserModel existingUser = organizationMapper.getUserByEmail(userModel.getEmail());
                if (existingUser != null && !existingUser.getId().equals(userModel.getId())) {
//                    UserModel emailUser = organizationMapper.getUserByEmail(userModel.getEmail());
//                    if (emailUser != null) {
                    throw new GlobalExceptionHandler.NotFoundException("Email already used");
//                    }
                }
            }

            int result;
            result = organizationMapper.updateOrganizationSelective(organization);
            if(result == 0){
                throw new GlobalExceptionHandler.NotFoundException("Fail to update organization");
            }
//            result = organizationMapper.updateUserByOrgId(userModel, organization.getId());
//            if(result == 0){
//                throw new GlobalExceptionHandler.NotFoundException("Fail to update organization");
//            }

            AuditLog auditLog = buildAuditLog(operator, "Organization edited", "organization", res, metadata);
//            auditRepository.save(auditLog);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(),
                    "Organization "+status.getUpdateDesc(),
                    "");

        } catch (Exception exception) {
            log.error("Error updating organization: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "editing organization");
            throw exception;
        }

    }

    @Override
    public Map<String, Object> addOrgModuleActivated(UUID orgId, Map<String, Boolean> module) {
        try {
            List<Map<String, Object>> results = new ArrayList<>();

            String dataMgmtValue = ModuleType.DATA_MANAGEMENT.getValue();
            XYZ existingDataMgmt = organizationMapper.getXyzByOrgAndModule(orgId, dataMgmtValue);
            if (existingDataMgmt == null) {
                XYZ xyz = new XYZ();
                xyz.setModule(dataMgmtValue);
                xyz.setStatus(true);
                xyz.setOrgId(orgId);
                organizationMapper.insertXyz(xyz);
                results.add(Map.of("module", "DATA_MANAGEMENT", "status", "activated"));
            } else {
                results.add(Map.of("module", "DATA_MANAGEMENT", "status", "already activated"));
            }

            for (Map.Entry<String, Boolean> entry : module.entrySet()) {
                String moduleName = entry.getKey();
                Boolean requestedStatus = entry.getValue();

                if (moduleName.equals("DATA_MANAGEMENT")) continue;

                ModuleType moduleType = ModuleType.fromName(moduleName);
                if (moduleType == null) {
                    throw new IllegalArgumentException("Invalid module type: " + moduleName);
                }

                String moduleValue = moduleType.getValue();
                XYZ existingXyz = organizationMapper.getXyzByOrgAndModule(orgId, moduleValue);

                if (existingXyz == null) {
                    XYZ xyz = new XYZ();
                    xyz.setModule(moduleValue);
                    xyz.setStatus(requestedStatus);
                    xyz.setOrgId(orgId);
                    organizationMapper.insertXyz(xyz);

                    String statusText = requestedStatus ? "activated" : "deactivated";
                    results.add(Map.of("module", moduleName, "status", statusText));
                    continue;
                }

                boolean currentStatus = existingXyz.isStatus();

                if (currentStatus == requestedStatus) {
                    String statusText = currentStatus ? "already activated" : "already deactivated";
                    results.add(Map.of("module", moduleName, "status", statusText));
                    continue;
                }
                System.out.println("requestedStatus: "+requestedStatus);
                System.out.println("existingXyz.getId(): "+existingXyz.getId());
                organizationMapper.updateXyzStatusById(existingXyz.getId(), requestedStatus);

                String statusText = requestedStatus ? "activated" : "deactivated";
                results.add(Map.of("module", moduleName, "status", statusText));
            }

            StringBuilder message = new StringBuilder();
            for (Map<String, Object> result : results) {
                String moduleName = (String) result.get("module");
                String status = (String) result.get("status");
                if (!moduleName.equals("DATA_MANAGEMENT")) {
                    if (message.length() > 0) message.append(", ");
                    message.append(moduleName).append(": ").append(status);
                }
            }

            return ResponseMap.response(status.getSuccessCode(), message.toString(), "");
        } catch (Exception exception) {
            log.error("Error activating module: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "activating module");
            throw exception;
        }
    }

    @Override
    public Map<String, Object> suspendOrganization(UUID id, Boolean suspend) {
        try {
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            Operator operator = handleUserValidation();
            Organization res = organizationMapper.getOrganizationById(id);

            if (res == null) {
                throw  new GlobalExceptionHandler.NotFoundException("Organization not found");
            }

            organizationMapper.suspendOrganization(id, suspend);

            Organization response = organizationMapper.getOrganizationById(id);

            String desc = response.getStatus() ? "Organization activated" : "Organization suspended";

            AuditLog auditLog = buildAuditLog(operator, desc, "organization", res, metadata);
//            auditRepository.save(auditLog);
            safeAuditService.saveAudit(auditLog);
            return ResponseMap.response(status.getSuccessCode(), desc + " successfully", "");

        } catch (Exception exception) {
            log.error("Error updating organization: {}", exception.getMessage(), exception);
            genericHandler.logAndSaveException(exception, "changing organization status");
            throw exception;
        }
    }


    private AuditLog buildAuditLog(Operator creator, String description, String type, Organization createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setOrganization(createdEntity);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndPoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }

}
