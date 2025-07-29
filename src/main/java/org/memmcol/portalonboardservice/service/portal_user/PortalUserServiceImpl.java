package org.memmcol.portalonboardservice.service.portal_user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class PortalUserServiceImpl implements PortalUserService {
    @Override
    public Map<String, Object> getAll() {
        return Map.of();
    }
}
