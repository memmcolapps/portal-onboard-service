package org.memmcol.portalonboardservice.util;

import org.memmcol.portalonboardservice.mapper.PortalUserMapper;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class handleValidUser {

    private static PortalUserMapper staticOperatorMapper;

    @Autowired
    public void setOperatorMapper(PortalUserMapper operatorMapper) {
        handleValidUser.staticOperatorMapper = operatorMapper;
    }

    public static Operator handleUserValidation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : null;

        if (username == null) {
            throw new UsernameNotFoundException("User not authenticated");
        }

        Operator user = staticOperatorMapper.findByAuthEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        if (!user.isStatus()) {
            throw new LockedException("User is blocked");
        }

        return user;
    }
}
