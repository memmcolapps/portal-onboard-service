package org.memmcol.portalonboardservice.service.organization;

import jakarta.transaction.Transactional;
import org.memmcol.portalonboardservice.mapper.OrganizationMapper;
import org.memmcol.portalonboardservice.mapper.UserMapper;
import org.memmcol.portalonboardservice.model.audit.ExceptionErrorLogs;
import org.memmcol.portalonboardservice.model.node.Node;
import org.memmcol.portalonboardservice.model.user.*;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.memmcol.portalonboardservice.util.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
            Map<String, Object> rootNodeResponse = creatRootNode(orgId, name);
            UUID rootNodeId = (UUID) ((Map<?, ?>) rootNodeResponse.get("data")).get("id");

            // Create Permissions
            createDefaultPermission(orgId);
            // Create Group
            createDefaultGroup(orgId);
            // Create Group Permissions
            createDefaultGroupPermission(orgId);
            // Create Default User
            createDefaultUser(orgId, rootNodeId, userModel);

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


    @Override
    public Map<String, Object> creatRootNode(UUID organizationId, String name) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();
        Node rootNode = new Node();

        try {

            rootNode.setName(name);
            rootNode.setOrgId(organizationId);

            organizationMapper.insertNodes(rootNode);

            Node savedNode = organizationMapper.getNodeByNameAndOrgId(name, organizationId);

            response.put("success", true);
            response.put("message", "Root Node created successfully");
            response.put("data", Map.of(
                    "id", savedNode.getId(),
                    "name", savedNode.getName()
            ));
            return response;
        } catch (Exception exception) {
            log.error("Error adding node: {}", exception.getMessage(), exception);

            errorLog.setDescription("Error creating Root Node");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Override
    public Map<String, Object> createDefaultUser(UUID organizationId, UUID nodeId, UserModel userModel) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();

        try {

            userModel.setOrgId(organizationId);
            userModel.setNodeId(nodeId);
            userModel.setStatus(true);
            userModel.setActive(true);

            organizationMapper.insertUser(userModel);

            return response;

        } catch (Exception exception) {
            log.error("Error creating default user: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error creating default user");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;

        }

    }

    @Override
    public Map<String, Object> createDefaultPermission(UUID organizationId) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();

        try {
            Permission permission = new Permission();

            permission.setView(true);
            permission.setEdit(true);
            permission.setApprove(true);
            permission.setDisable(true);
            permission.setOrgId(organizationId);

            organizationMapper.insertPermission(permission);

            response.put("success", true);
            response.put("message", "Permission created successfully");
            response.put("data", permission);
            return response;

        } catch (Exception exception) {
            log.error("Error creating default Permission: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error creating default Permission");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Override
    public Map<String, Object> createDefaultGroup(UUID organizationId) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();

        try {
            Group group = new Group();

            group.setGroupTitle("Full access");
            group.setOrgId(organizationId);

            organizationMapper.insertGroup(group);

            response.put("success", true);
            response.put("message", "Default Group created successfully");
            response.put("data", group);
            return response;

        } catch (Exception exception) {
            log.error("Error creating default Group: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error creating default Group");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Override
    public Map<String, Object> createDefaultGroupPermission(UUID organizationId) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        Map<String, Object> response = new HashMap<>();

        try {

            Permission permission = organizationMapper.getPermissionByOrgId(organizationId);
            Group group = organizationMapper.getGroupByOrgId(organizationId);

            organizationMapper.insertGroupPermission(group.getId(), permission.getId(), organizationId);

            response.put("success", true);
            response.put("message", "Default Group Permission created successfully");
            return response;

        } catch (Exception exception) {
            log.error("Error creating default Group Permission: {}", exception.getMessage(), exception);

            // Log error details
            errorLog.setDescription("Error creating default Group Permission");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;
        }
    }

    @Override
    public Map<String, Object> getOrganization(int page, int size) {
        try {

            // Calculate offset
            int offset = page * size;

            List<Organization> organizations;
            // Get paginated data
            if(size == 0){
                organizations = organizationMapper.getAllOrganizations();
            } else {
                organizations = organizationMapper.getOrganizations(size, offset);
            }


            long totalCount = organizationMapper.getOrganizationCount();
            int totalPages = (int) Math.ceil((double) totalCount / size);

            Map<String, Object> paginationData = new HashMap<>();
            paginationData.put("content", organizations);
            paginationData.put("pageNumber", page);
            paginationData.put("pageSize", size);
            paginationData.put("totalElements", totalCount);
            paginationData.put("totalPages", totalPages);
            paginationData.put("isFirst", page == 0);
            paginationData.put("isLast", (offset + size) >= totalCount);

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Organizations retrieved successfully",
                    paginationData
            );

        } catch (Exception exception) {
            log.error("Error fetching organizations - Page: {}, Size: {}: {}",
                    page, size, exception.getMessage(), exception);

            ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
            errorLog.setDescription("Error fetching organizations");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;

//            return ResponseMap.response(
//                    status.getFailCode(),
//                    "Failed to fetch organizations",
//                    Map.of(
//                            "error", exception.getMessage(),
//                            "page", page,
//                            "size", size
//                    )
//            );
        }
    }

    @Override
    public Map<String, Object> getOrganizationById(UUID id) {
        try {
            Optional<Organization> result = organizationMapper.getOrganizationById(id);

            if (result.isEmpty()) {
                return ResponseMap.response(
                        status.getNotFoundCode(),
                        "Organization not found with ID: " + id,
                        Map.of("organizationId", id)
                );
            }

            return ResponseMap.response(
                    status.getSuccessCode(),
                    "Organization retrieved successfully",
                    result
            );

        } catch (Exception exception) {
            log.error("Error fetching organization {}: {}", id, exception.getMessage(), exception);

            ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
            errorLog.setDescription("Error fetching organization");
            errorLog.setError_message(exception.getMessage());
            errorLog.setError(exception.toString());
            exceptionAuditRepository.save(errorLog);

            throw exception;

//            return ResponseMap.response(
//                    status.getFailCode(),
//                    "Failed to fetch organization",
//                    Map.of(
//                            "error", exception.getMessage(),
//                            "organizationId", id
//                    )
//            );
        }
    }

    @Override
    public Map<String, Object> updateOrganization(Organization organization,UserModel userModel, UUID orgId) {

        ExceptionErrorLogs errorLog = new ExceptionErrorLogs();
        try {
            Organization originalData = organizationMapper.getOrganizationById(orgId)
                    .orElseThrow(()-> new RuntimeException("Organization not found with ID: " + orgId));

//            UserModel user = organizationMapper.getUserByOrgId(orgId);
            organization.setId(orgId);
            organizationMapper.updateOrganizationSelective(organization);
            organizationMapper.updateUserByOrgId(orgId,userModel.getEmail(),
                    userModel.getPhoneNumber(),
                    userModel.getFirstname(),
                    userModel.getLastname());

            System.out.println("############################"+ userModel.getEmail());


            Organization updatedData = organizationMapper.getOrganizationById(orgId)
                    .orElseThrow(()-> new RuntimeException("Organization not found with ID: " + orgId));

            Map<String, Map<String, String>> changes = new HashMap<>();

            addChangeIfDifferent("businessName", originalData.getBusinessName(), updatedData.getBusinessName(), changes);
            addChangeIfDifferent("businessType", originalData.getPostalCode(), updatedData.getPostalCode(), changes);
            addChangeIfDifferent("registrationNumber", originalData.getAddress(), updatedData.getAddress(), changes);
            addChangeIfDifferent("country", originalData.getCountry(), updatedData.getCountry(), changes);
            addChangeIfDifferent("state", originalData.getState(), updatedData.getState(), changes);
            addChangeIfDifferent("city", originalData.getCity(), updatedData.getCity(), changes);


            return ResponseMap.response(status.getSuccessCode(),
                    "Organization updated Successfully",
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
