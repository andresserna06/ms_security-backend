package com.uc.ms_security.service;

import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class RolePermissionAuthorizationService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String FORBIDDEN_MESSAGE =
            "No tienes permisos para modificar este rol";

    public void ensureCanManage(Collection<String> roles) {
        if (roles == null || roles.stream().noneMatch(this::isAdminRole)) {
            throw forbidden();
        }
    }

    private boolean isAdminRole(String role) {
        return role != null
                && ADMIN_ROLE.equalsIgnoreCase(
                        role.replaceFirst("(?i)^ROLE_", "").trim());
    }

    private ApplicationException forbidden() {
        return new ApplicationException(
                ErrorCase.FORBIDDEN,
                FORBIDDEN_MESSAGE
        );
    }
}
