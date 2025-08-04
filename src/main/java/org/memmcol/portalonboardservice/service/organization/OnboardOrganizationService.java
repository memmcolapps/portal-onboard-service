package org.memmcol.portalonboardservice.service.organization;

import org.memmcol.portalonboardservice.model.user.OnboardingOrganizationDTO;
import org.memmcol.portalonboardservice.model.user.Organization;
import org.memmcol.portalonboardservice.model.user.UserModel;

import java.util.Map;
import java.util.UUID;

public interface OnboardOrganizationService {
    Map<String, Object> addOrganization(Organization organization, UserModel userModel);
    Map<String, Object> creatRootNode(UUID organizationId, String name);
    Map<String, Object> createDefaultUser(UUID organizationId, UUID nodeId, UserModel userModel);
    Map<String, Object> createDefaultPermission(UUID organizationId);
    Map<String, Object> createDefaultGroup(UUID organizationId);
    Map<String, Object> createDefaultGroupPermission(UUID organizationId);
    Map<String, Object> getOrganization(int page,int size);
    Map<String, Object> getOrganizationById(UUID id);
    Map<String, Object> updateOrganization(Organization organization,UserModel UserModel, UUID orgId);
}
