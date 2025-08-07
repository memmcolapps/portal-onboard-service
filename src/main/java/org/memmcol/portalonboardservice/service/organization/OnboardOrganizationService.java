package org.memmcol.portalonboardservice.service.organization;

import org.memmcol.portalonboardservice.model.user.Organization;
import org.memmcol.portalonboardservice.model.user.UserModel;

import java.util.Map;
import java.util.UUID;

public interface OnboardOrganizationService {
    Map<String, Object> addOrganization(Organization organization, UserModel userModel);
    Map<String, Object> getOrganization();
    Map<String, Object> getOrganizationById(UUID id);
    Map<String, Object> updateOrganization(Organization organization,UserModel UserModel);
}
