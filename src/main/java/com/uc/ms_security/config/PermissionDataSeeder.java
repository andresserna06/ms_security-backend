package com.uc.ms_security.config;

import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.RolePermission;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PermissionDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PermissionDataSeeder.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        Role adminRole = getOrCreateAdminRole();
        assignAllPermissions(adminRole);
        seedAdminUser(adminRole);
    }

    private void seedPermissions() {
        for (PermissionSeed seed : permissionSeeds()) {
            if (permissionRepository.existsByUrlAndMethod(seed.url(), seed.method())) {
                continue;
            }

            Permission permission = new Permission();
            permission.setUrl(seed.url());
            permission.setMethod(seed.method());
            permissionRepository.save(permission);
        }
    }

    private Role getOrCreateAdminRole() {
        return roleRepository.findAll().stream()
                .filter(role -> "ADMIN".equalsIgnoreCase(role.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ADMIN");
                    role.setDescription("Administrador con acceso total al sistema");
                    return roleRepository.save(role);
                });
    }

    private void assignAllPermissions(Role adminRole) {
        for (Permission permission : permissionRepository.findAll()) {
            if (rolePermissionRepository.existsByRoleIdAndPermissionId(
                    adminRole.getId(),
                    permission.getId())) {
                continue;
            }

            RolePermission rolePermission = new RolePermission();
            rolePermission.setRole(adminRole);
            rolePermission.setPermission(permission);
            rolePermissionRepository.save(rolePermission);
        }
    }

    private void seedAdminUser(Role adminRole) {
        String configuredEmail = adminEmail == null ? "" : adminEmail.trim();
        String configuredPassword = adminPassword == null ? "" : adminPassword;

        if (configuredEmail.isEmpty() && configuredPassword.isEmpty()) {
            logger.warn(
                    "Bootstrap ADMIN user was not created. Configure APP_BOOTSTRAP_ADMIN_EMAIL and "
                            + "APP_BOOTSTRAP_ADMIN_PASSWORD to create it.");
            return;
        }
        if (configuredEmail.isEmpty() || configuredPassword.isBlank()) {
            throw new IllegalStateException(
                    "Both APP_BOOTSTRAP_ADMIN_EMAIL and APP_BOOTSTRAP_ADMIN_PASSWORD must be configured.");
        }

        User admin = userRepository.findByEmail(configuredEmail)
                .orElseGet(() -> createAdminUser(configuredEmail, configuredPassword));

        if (!userRoleRepository.existsByUserIdAndRoleId(admin.getId(), adminRole.getId())) {
            UserRole userRole = new UserRole();
            userRole.setUser(admin);
            userRole.setRole(adminRole);
            userRoleRepository.save(userRole);
        }
    }

    private User createAdminUser(String email, String rawPassword) {
        User admin = new User();
        admin.setName("Administrador");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setActive(true);
        admin.setAuthProvider(AuthProvider.LOCAL);
        return userRepository.save(admin);
    }

    private List<PermissionSeed> permissionSeeds() {
        return List.of(
                new PermissionSeed("POST", "/api/users"),
                new PermissionSeed("GET", "/api/users"),
                new PermissionSeed("GET", "/api/users/{id}"),
                new PermissionSeed("GET", "/api/users/{id}/detail"),
                new PermissionSeed("GET", "/api/users/{id}/detail-with-sessions"),
                new PermissionSeed("GET", "/api/users/{id}/detail-with-roles"),
                new PermissionSeed("PUT", "/api/users/{id}"),
                new PermissionSeed("PATCH", "/api/users/{id}/deactivate"),
                new PermissionSeed("PATCH", "/api/users/{id}/activate"),
                new PermissionSeed("DELETE", "/api/users/{id}"),
                new PermissionSeed("GET", "/api/users/{userId}/profile"),
                new PermissionSeed("POST", "/api/users/{userId}/profile"),
                new PermissionSeed("PUT", "/api/users/{userId}/profile"),
                new PermissionSeed("DELETE", "/api/users/{userId}/profile"),
                new PermissionSeed("POST", "/api/users/{userId}/sessions"),
                new PermissionSeed("GET", "/api/users/{userId}/sessions"),
                new PermissionSeed("GET", "/api/users/{userId}/sessions/{sessionId}"),
                new PermissionSeed("PUT", "/api/users/{userId}/sessions/{sessionId}"),
                new PermissionSeed("DELETE", "/api/users/{userId}/sessions/{sessionId}"),
                new PermissionSeed("POST", "/api/roles"),
                new PermissionSeed("GET", "/api/roles"),
                new PermissionSeed("GET", "/api/roles/{id}"),
                new PermissionSeed("GET", "/api/roles/{id}/detail-with-permissions"),
                new PermissionSeed("PUT", "/api/roles/{id}"),
                new PermissionSeed("DELETE", "/api/roles/{id}"),
                new PermissionSeed("POST", "/api/permissions"),
                new PermissionSeed("GET", "/api/permissions"),
                new PermissionSeed("GET", "/api/permissions/{id}"),
                new PermissionSeed("PUT", "/api/permissions/{id}"),
                new PermissionSeed("DELETE", "/api/permissions/{id}"),
                new PermissionSeed("POST", "/api/role-permissions"),
                new PermissionSeed("GET", "/api/role-permissions"),
                new PermissionSeed("GET", "/api/role-permissions/roles/{roleId}"),
                new PermissionSeed("GET", "/api/role-permissions/permissions/{permissionId}"),
                new PermissionSeed("DELETE", "/api/role-permissions/{roleId}/{permissionId}"),
                new PermissionSeed("POST", "/api/user-roles"),
                new PermissionSeed("GET", "/api/user-roles"),
                new PermissionSeed("GET", "/api/user-roles/users/{userId}"),
                new PermissionSeed("GET", "/api/user-roles/roles/{roleId}"),
                new PermissionSeed("DELETE", "/api/user-roles/{userId}/{roleId}")
        );
    }

    private record PermissionSeed(String method, String url) {
    }
}
