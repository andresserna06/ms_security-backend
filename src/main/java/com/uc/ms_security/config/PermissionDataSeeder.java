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
import java.util.Locale;

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

    @Value("${security.oauth.default-role:USER}")
    private String defaultUserRoleName;

    @Override
    @Transactional
    public void run(String... args) {
        String defaultRoleName = resolveDefaultUserRoleName();
        List<Permission> securityPermissions = seedPermissions(securityPermissionSeeds());
        List<Permission> sportPermissions = seedPermissions(sportPermissionSeeds());
        List<Permission> defaultUserPermissions = seedPermissions(defaultUserPermissionSeeds());

        Role adminRole = ensureRole(
                "ADMIN",
                "Administrador del Sistema con acceso a gestión de seguridad");
        Role sportsAdminRole = ensureRole(
                "SPORTS_ADMIN",
                "Administrador deportivo: gestiona deportes, competencias e infraestructura deportiva");
        Role defaultUserRole = ensureRole(
                defaultRoleName,
                "Usuario autenticado con acceso de consulta básica");

        assignPermissions(adminRole, securityPermissions);
        assignPermissions(sportsAdminRole, sportPermissions);
        assignPermissions(defaultUserRole, defaultUserPermissions);
        seedAdminUser(adminRole);
        backfillDefaultRole(defaultUserRole);
    }

    private String resolveDefaultUserRoleName() {
        if (defaultUserRoleName == null || defaultUserRoleName.isBlank()) {
            return "USER";
        }
        return defaultUserRoleName.trim().toUpperCase(Locale.ROOT);
    }

    private void backfillDefaultRole(Role defaultUserRole) {
        for (User user : userRepository.findAll()) {
            if (userRoleRepository.existsByUserId(user.getId())) {
                continue;
            }

            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(defaultUserRole);
            userRoleRepository.save(userRole);
        }
    }

    private List<Permission> seedPermissions(List<PermissionSeed> seeds) {
        return seeds.stream()
                .map(this::getOrCreatePermission)
                .toList();
    }

    private Permission getOrCreatePermission(PermissionSeed seed) {
        return permissionRepository.findByUrlAndMethod(seed.url(), seed.method())
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setUrl(seed.url());
                    permission.setMethod(seed.method());
                    return permissionRepository.save(permission);
                });
    }

    private Role ensureRole(String name, String description) {
        return roleRepository.findAll().stream()
                .filter(role -> name.equalsIgnoreCase(role.getName()))
                .findFirst()
                .map(role -> {
                    if (description != null && !description.equals(role.getDescription())) {
                        role.setDescription(description);
                        return roleRepository.save(role);
                    }
                    return role;
                })
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }

    private void assignPermissions(Role role, List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (rolePermissionRepository.existsByRoleIdAndPermissionId(
                    role.getId(),
                    permission.getId())) {
                continue;
            }

            RolePermission rolePermission = new RolePermission();
            rolePermission.setRole(role);
            rolePermission.setPermission(permission);
            rolePermissionRepository.save(rolePermission);
        }
    }

    private List<PermissionSeed> defaultUserPermissionSeeds() {
        return List.of(
                new PermissionSeed("GET", "/teams"),
                new PermissionSeed("GET", "/teams/{id}"),
                new PermissionSeed("GET", "/players"),
                new PermissionSeed("GET", "/players/{id}"),
                new PermissionSeed("GET", "/tournaments"),
                new PermissionSeed("GET", "/tournaments/{id}")
        );
    }

    private List<PermissionSeed> securityPermissionSeeds() {
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

        private List<PermissionSeed> sportPermissionSeeds() {
        return List.of(
                new PermissionSeed("GET", "/sports"),
                new PermissionSeed("GET", "/sports/{id}"),
                new PermissionSeed("GET", "/sports/{id}/subsports"),
                new PermissionSeed("POST", "/sports"),
                new PermissionSeed("PUT", "/sports/{id}"),
                new PermissionSeed("PATCH", "/sports/{id}/deactivate"),
                new PermissionSeed("PATCH", "/sports/{id}/activate"),
                new PermissionSeed("DELETE", "/sports/{id}"),
                new PermissionSeed("GET", "/teams"),
                new PermissionSeed("GET", "/teams/{id}"),
                new PermissionSeed("GET", "/teams/reports/active-by-city"),
                new PermissionSeed("POST", "/teams"),
                new PermissionSeed("PUT", "/teams/{id}"),
                new PermissionSeed("PATCH", "/teams/{id}/deactivate"),
                new PermissionSeed("PATCH", "/teams/{id}/activate"),
                new PermissionSeed("DELETE", "/teams/{id}"),
                new PermissionSeed("POST", "/teams/{teamId}/sports/{sportId}"),
                new PermissionSeed("DELETE", "/teams/{teamId}/sports/{sportId}"),
                new PermissionSeed("GET", "/teams/{teamId}/sports"),
                new PermissionSeed("GET", "/sports/{sportId}/teams"),
                new PermissionSeed("GET", "/skills"),
                new PermissionSeed("GET", "/skills/{id}"),
                new PermissionSeed("POST", "/skills"),
                new PermissionSeed("PUT", "/skills/{id}"),
                new PermissionSeed("DELETE", "/skills/{id}"),
                new PermissionSeed("GET", "/players"),
                new PermissionSeed("GET", "/players/{id}"),
                new PermissionSeed("POST", "/players"),
                new PermissionSeed("PUT", "/players/{id}"),
                new PermissionSeed("PATCH", "/players/{id}/deactivate"),
                new PermissionSeed("PATCH", "/players/{id}/activate"),
                new PermissionSeed("DELETE", "/players/{id}"),
                new PermissionSeed("POST", "/teams/{teamId}/contracts"),
                new PermissionSeed("PATCH", "/contracts/{id}/end"),
                new PermissionSeed("GET", "/teams/{teamId}/players"),
                new PermissionSeed("GET", "/players/{playerId}/teams"),
                new PermissionSeed("POST", "/players/{playerId}/skills/{skillId}"),
                new PermissionSeed("DELETE", "/players/{playerId}/skills/{skillId}"),
                new PermissionSeed("GET", "/players/{playerId}/skills"),
                new PermissionSeed("GET", "/skills/{skillId}/players"),
                new PermissionSeed("GET", "/players/{playerId}/profile"),
                new PermissionSeed("GET", "/tournaments"),
                new PermissionSeed("GET", "/tournaments/{id}"),
                new PermissionSeed("GET", "/tournaments/{id}/overview"),
                new PermissionSeed("GET", "/tournaments/{id}/phases"),
                new PermissionSeed("GET", "/tournaments/reports/open-enrollment"),
                new PermissionSeed("POST", "/tournaments"),
                new PermissionSeed("PUT", "/tournaments/{id}"),
                new PermissionSeed("PATCH", "/tournaments/{id}/deactivate"),
                new PermissionSeed("PATCH", "/tournaments/{id}/activate"),
                new PermissionSeed("DELETE", "/tournaments/{id}"),
                new PermissionSeed("POST", "/tournaments/{tournamentId}/registrations"),
                new PermissionSeed("GET", "/tournaments/{tournamentId}/registrations"),
                new PermissionSeed("GET", "/teams/{teamId}/tournaments"),
                new PermissionSeed("PATCH", "/registrations/{id}/confirm"),
                new PermissionSeed("PATCH", "/registrations/{id}/withdraw"),
                new PermissionSeed("PATCH", "/tournaments/{tournamentId}/registrations/{teamId}/confirm"),
                new PermissionSeed("PATCH", "/tournaments/{tournamentId}/registrations/{teamId}/withdraw"),
                new PermissionSeed("GET", "/phases"),
                new PermissionSeed("GET", "/phases/{id}"),
                new PermissionSeed("GET", "/phases/reports/count-by-type"),
                new PermissionSeed("GET", "/groups"),
                new PermissionSeed("GET", "/groups/{id}"),
                new PermissionSeed("GET", "/groups/{id}/teams"),
                new PermissionSeed("POST", "/groups"),
                new PermissionSeed("PATCH", "/groups/{id}"),
                new PermissionSeed("POST", "/groups/{groupId}/teams/{teamId}"),
                new PermissionSeed("DELETE", "/groups/{groupId}/teams/{teamId}"),
                new PermissionSeed("DELETE", "/groups/{id}"),
                new PermissionSeed("GET", "/finals"),
                new PermissionSeed("GET", "/finals/{id}"),
                new PermissionSeed("POST", "/finals"),
                new PermissionSeed("PATCH", "/finals/{id}"),
                new PermissionSeed("DELETE", "/finals/{id}"),
                new PermissionSeed("POST", "/phases/{phaseId}/matches"),
                new PermissionSeed("GET", "/matches"),
                new PermissionSeed("GET", "/phases/{phaseId}/matches"),
                new PermissionSeed("GET", "/matches/{id}"),
                new PermissionSeed("PATCH", "/matches/{id}"),
                new PermissionSeed("PATCH", "/matches/{id}/start"),
                new PermissionSeed("DELETE", "/matches/{id}"),
                new PermissionSeed("PUT", "/matches/{id}/result"),
                new PermissionSeed("PATCH", "/matches/{id}/finish"),
                new PermissionSeed("POST", "/matches/{id}/scoring-events"),
                new PermissionSeed("GET", "/matches/{id}/scoring-events"),
                new PermissionSeed("PUT", "/scoring-events/{id}"),
                new PermissionSeed("DELETE", "/scoring-events/{id}"),
                new PermissionSeed("GET", "/venues"),
                new PermissionSeed("GET", "/venues/{id}"),
                new PermissionSeed("POST", "/venues"),
                new PermissionSeed("PUT", "/venues/{id}"),
                new PermissionSeed("DELETE", "/venues/{id}"),
                new PermissionSeed("GET", "/seating-zones"),
                new PermissionSeed("GET", "/seating-zones/{id}"),
                new PermissionSeed("POST", "/seating-zones"),
                new PermissionSeed("PUT", "/seating-zones/{id}"),
                new PermissionSeed("DELETE", "/seating-zones/{id}"),
                new PermissionSeed("GET", "/tournament-venues"),
                new PermissionSeed("GET", "/tournament-venues/{id}"),
                new PermissionSeed("POST", "/tournament-venues"),
                new PermissionSeed("PUT", "/tournament-venues/{id}"),
                new PermissionSeed("DELETE", "/tournament-venues/{id}"),
                new PermissionSeed("GET", "/fans"),
                new PermissionSeed("GET", "/fans/{id}"),
                new PermissionSeed("POST", "/fans"),
                new PermissionSeed("PUT", "/fans/{id}"),
                new PermissionSeed("DELETE", "/fans/{id}"),
                new PermissionSeed("GET", "/fan-teams"),
                new PermissionSeed("GET", "/fan-teams/{id}"),
                new PermissionSeed("POST", "/fan-teams"),
                new PermissionSeed("PUT", "/fan-teams/{id}"),
                new PermissionSeed("DELETE", "/fan-teams/{id}"),
                new PermissionSeed("GET", "/payment-methods"),
                new PermissionSeed("GET", "/payment-methods/{id}"),
                new PermissionSeed("POST", "/payment-methods"),
                new PermissionSeed("PUT", "/payment-methods/{id}"),
                new PermissionSeed("DELETE", "/payment-methods/{id}"),
                new PermissionSeed("GET", "/fan-payment-methods"),
                new PermissionSeed("GET", "/fan-payment-methods/{id}"),
                new PermissionSeed("POST", "/fan-payment-methods"),
                new PermissionSeed("PUT", "/fan-payment-methods/{id}"),
                new PermissionSeed("DELETE", "/fan-payment-methods/{id}"),
                new PermissionSeed("GET", "/tickets"),
                new PermissionSeed("GET", "/tickets/{id}"),
                new PermissionSeed("POST", "/tickets"),
                new PermissionSeed("PUT", "/tickets/{id}"),
                new PermissionSeed("DELETE", "/tickets/{id}"),
                new PermissionSeed("GET", "/bets"),
                new PermissionSeed("GET", "/bets/{id}"),
                new PermissionSeed("POST", "/bets"),
                new PermissionSeed("PUT", "/bets/{id}"),
                new PermissionSeed("DELETE", "/bets/{id}"),
                new PermissionSeed("GET", "/balls"),
                new PermissionSeed("GET", "/balls/{id}"),
                new PermissionSeed("POST", "/balls"),
                new PermissionSeed("PUT", "/balls/{id}"),
                new PermissionSeed("DELETE", "/balls/{id}"),
                new PermissionSeed("GET", "/spatial-positions"),
                new PermissionSeed("GET", "/spatial-positions/{id}"),
                new PermissionSeed("POST", "/spatial-positions"),
                new PermissionSeed("PUT", "/spatial-positions/{id}"),
                new PermissionSeed("DELETE", "/spatial-positions/{id}")
        );
    }

    private record PermissionSeed(String method, String url) {
    }
}