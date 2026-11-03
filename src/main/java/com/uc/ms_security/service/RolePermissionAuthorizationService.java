package com.uc.ms_security.service;

import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RolePermissionAuthorizationService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String FORBIDDEN_MESSAGE =
            "No tienes permisos para modificar este rol";

    private final UserRepository userRepository;
    private final PermissionAuthorizationService permissionAuthorizationService;

    public void ensureCanManage(
            Long userId,
            Long targetRoleId,
            String requestUrl,
            String requestMethod
    ) {
        if (userId == null || targetRoleId == null) {
            throw forbidden();
        }

        if (!permissionAuthorizationService.hasPermission(
                userId,
                requestUrl,
                requestMethod)) {
            throw forbidden();
        }

        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(this::forbidden);

        if (!isAdmin(user)) {
            throw forbidden();
        }
    }

    private boolean isAdmin(User user) {
        if (user.getUserRoles() == null) {
            return false;
        }

        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(name -> name.replaceFirst("(?i)^ROLE_", "").trim())
                .anyMatch(ADMIN_ROLE::equalsIgnoreCase);
    }

    private ApplicationException forbidden() {
        return new ApplicationException(
                ErrorCase.FORBIDDEN,
                FORBIDDEN_MESSAGE
        );
    }
}
