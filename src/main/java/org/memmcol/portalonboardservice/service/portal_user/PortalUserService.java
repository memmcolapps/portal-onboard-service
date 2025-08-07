package org.memmcol.portalonboardservice.service.portal_user;

import java.util.Map;

public interface PortalUserService {

    Map<String, Object> getAll();

    Map<String, Object> logout();
}
