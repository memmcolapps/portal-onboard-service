package org.memmcol.portalonboardservice.service.organization;

import jakarta.transaction.Transactional;
import org.memmcol.portalonboardservice.mapper.OrganizationMapper;
import org.memmcol.portalonboardservice.mapper.UserMapper;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.node.Node;
import org.memmcol.portalonboardservice.model.user.*;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.memmcol.portalonboardservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;


@Service
@Transactional
public class OnboardOrganizationServiceImpl implements OnboardOrganizationService {


    private final OrganizationMapper organizationMapper;
    private final ExceptionAuditRepository exceptionAuditRepository;
    private static final Logger log = LoggerFactory.getLogger(OnboardOrganizationServiceImpl.class);
    private final UserMapper userMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Other mappers can be added as needed
    public OnboardOrganizationServiceImpl(OrganizationMapper organizationMapper,
                                          ExceptionAuditRepository exceptionAuditRepository,
                                          UserMapper userMapper) {
        this.organizationMapper = organizationMapper;
        this.exceptionAuditRepository = exceptionAuditRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Map<String, Object> addOrganization(Organization organization, UserModel userModel) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();

        try {

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

            // Create Permissions
            int result;
            result = createDefaultPermission(orgId);
            if (result == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Fail to create permission");
            }

            // Create Group
            result = createDefaultGroup(orgId);
            if (result == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Fail to create group");
            }
            // Create Group Permissions
            result = createDefaultGroupPermission(orgId);
            if (result == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Fail to create group permission");
            }
            // Create Default User
            result = createDefaultUser(orgId, rootNodeId, userModel);
            if (result == 0) {
                throw new GlobalExceptionHandler.NotFoundException("Fail to create user");
            }

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

    private int createDefaultUser(UUID organizationId, UUID nodeId, UserModel userModel) {

            userModel.setOrgId(organizationId);
            userModel.setNodeId(nodeId);
            userModel.setStatus(true);
            userModel.setActive(true);
            int result;
            result = organizationMapper.insertUser(userModel);
            UUID id = userModel.getId();
            organizationMapper.updateOrg(id, organizationId);
            return result;

    }

    private int createDefaultPermission(UUID organizationId) {
            Permission permission = new Permission();

            permission.setView(true);
            permission.setEdit(true);
            permission.setApprove(true);
            permission.setDisable(true);
            permission.setOrgId(organizationId);

            int result;
            result = organizationMapper.insertPermission(permission);
            return result;
    }

    private int createDefaultGroup(UUID organizationId) {
            Group group = new Group();

            group.setGroupTitle("User management");
            group.setOrgId(organizationId);

            int result;
            result = organizationMapper.insertGroup(group);
            return result;
    }

    public int createDefaultGroupPermission(UUID organizationId) {

            Permission permission = organizationMapper.getPermissionByOrgId(organizationId);
            Group group = organizationMapper.getGroupByOrgId(organizationId);

            int response;
            response = organizationMapper.insertGroupPermission(group.getId(), permission.getId(), organizationId);
            return response;
    }

    @Override
    public Map<String, Object> getOrganization() {
        try {

            List<Organization> organizations = organizations = organizationMapper.getAllOrganizations();

            int totalOrganizations = organizations.size();
            int totalActiveOrganizations = 0;
            BigDecimal totalVending = BigDecimal.ZERO;
            BigDecimal totalBilling = BigDecimal.ZERO;
            int overallCustomers = 0;
            BigDecimal overallVending = BigDecimal.ZERO;
            BigDecimal overallBilling = BigDecimal.ZERO;
//            private Integer customerCount;
//            private BigDecimal totalVending;
//            private BigDecimal totalBilling;


            for (Organization org : organizations) {
                if (org.getStatus()) {
                    totalActiveOrganizations++;
                }

                // Build node tree...
                List<Node> nodes = organizationMapper.getNodeWithChildren(org.getOperator().getNodeId(), org.getId());
                Map<UUID, Node> nodeMap = new HashMap<>();
                Node root = null;

                for (Node node : nodes) {
                    node.setNodesTree(new ArrayList<>());
                    nodeMap.put(node.getId(), node);
                }

                for (Node node : nodes) {
                    if (node.getId().equals(org.getOperator().getNodeId())) {
                        root = node;
                    }
                    if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
                        nodeMap.get(node.getParentId()).getNodesTree().add(node);
                    }
                }

                org.getOperator().setNodes(root);

                // Get per-org metrics
                Long orgCustomerCount = organizationMapper.totalCustomer(org.getId());
                BigDecimal orgVendingTotal = BigDecimal.valueOf(0);
                BigDecimal orgBillingTotal = BigDecimal.valueOf(0);

                // Set in organization object
                org.setCustomerCount(orgCustomerCount);
                org.setTotalVending(orgVendingTotal != null ? orgVendingTotal : BigDecimal.ZERO);
                org.setTotalBilling(orgBillingTotal != null ? orgBillingTotal : BigDecimal.ZERO);

                // Accumulate for global totals
                overallCustomers += orgCustomerCount;
                overallVending = totalVending.add(org.getTotalVending());
                overallBilling = totalBilling.add(org.getTotalBilling());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalOrganizations", totalOrganizations);
            response.put("totalActiveOrganizations", totalActiveOrganizations);
            response.put("overallCustomers", overallCustomers);
            response.put("overallVending", overallVending);
            response.put("overallBilling", overallBilling);
            response.put("organizations", organizations);

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

    @Override
    public Map<String, Object> getOrganizationById(UUID id) {
        try {

            Organization result = organizationMapper.getOrganizationById(id);

            if (result == null) {
                throw  new GlobalExceptionHandler.NotFoundException("Organization not found");
            }

            Long customer = organizationMapper.totalCustomer(result.getId());

            BigDecimal vending = BigDecimal.valueOf(0);

            BigDecimal billing = BigDecimal.valueOf(0);

            List<Node> nodes = organizationMapper.getNodeWithChildren(result.getOperator().getNodeId(), result.getId());

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
            result.getOperator().setNodes(root);
            Map<String, Object> response = new HashMap<>();
            response.put("organization", result);
            response.put("totalCustomer", customer);
            response.put("totalVending", vending);
            response.put("totalBilling", billing);

            return ResponseMap.response(status.getSuccessCode(), status.getDesc(), response);

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

    @Override
    public Map<String, Object> updateOrganization(Organization organization,UserModel userModel) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        try {
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
