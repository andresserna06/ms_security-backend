package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.entity.Session;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.SessionRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.service.oauth.OAuthProviderStrategy;
import com.uc.ms_security.service.oauth.OAuthUserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta la autenticación con proveedores externos (OAuth).
 * Depende únicamente de la abstracción OAuthProviderStrategy (DIP): Spring inyecta
 * todas las implementaciones disponibles y aquí se indexan por proveedor.
 */
@Service
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private final Map<AuthProvider, OAuthProviderStrategy> strategies;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SessionRepository sessionRepository;
    private final JwtService jwtService;
    private final String defaultRoleName;

    public OAuthService(
            List<OAuthProviderStrategy> providerStrategies,
            UserRepository userRepository,
            RoleRepository roleRepository,
            SessionRepository sessionRepository,
            JwtService jwtService,
            @Value("${security.oauth.default-role:USER}") String defaultRoleName) {

        this.strategies = new EnumMap<>(AuthProvider.class);
        providerStrategies.forEach(strategy ->
                this.strategies.put(strategy.getProviderName(), strategy));

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRepository = sessionRepository;
        this.jwtService = jwtService;
        this.defaultRoleName = defaultRoleName;
    }

    @Transactional
    public AuthResponseDTO loginWithProvider(AuthProvider provider, String idToken) {
        OAuthProviderStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "Proveedor de autenticación no soportado: " + provider
            );
        }

        // 1. Validar identidad con el proveedor externo
        OAuthUserProfile profile = strategy.verifyAndExtractUser(idToken);

        // 2. Identificar o aprovisionar al usuario
        User user = userRepository
                .findWithRolesAndPermissionsByEmail(profile.email())
                .map(existing -> validateExistingUser(existing, provider))
                .orElseGet(() -> provisionUser(profile, provider));

        // 3. Emitir sesión interna (sin 2FA para OAuth)
        AuthResponseDTO authResponse = jwtService.generateToken(user);

        Session session = new Session();
        session.setUser(user);
        session.setToken(authResponse.accessToken());
        session.setExpiration(LocalDateTime.ofInstant(
                authResponse.expiresAt(), ZoneId.systemDefault()));
        session.setCodeUsed(true);
        sessionRepository.save(session);

        return authResponse;
    }

    private User validateExistingUser(User user, AuthProvider provider) {
        // Criterio 11: correo registrado con autenticación básica -> rechazar
        if (user.getAuthProvider() == AuthProvider.LOCAL) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El correo ya fue registrado mediante autenticación directa. Inicie sesión con su contraseña."
            );
        }

        // Correo asociado a otro proveedor externo distinto
        if (user.getAuthProvider() != provider) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El correo ya está asociado a otro proveedor de autenticación: " + user.getAuthProvider()
            );
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El usuario se encuentra inhabilitado"
            );
        }

        // Criterios 12 y 13: acceso posterior, se reutiliza el usuario sin duplicar
        return user;
    }

    private User provisionUser(OAuthUserProfile profile, AuthProvider provider) {
        // Criterio 10: primer acceso, creación automática
        User user = new User();
        user.setEmail(profile.email());
        user.setName(resolveName(profile));
        user.setPassword(null);
        user.setActive(true);
        user.setAuthProvider(provider);

        roleRepository.findByNameIgnoreCase(defaultRoleName).ifPresentOrElse(
                role -> {
                    UserRole userRole = new UserRole();
                    userRole.setUser(user);
                    userRole.setRole(role);
                    user.getUserRoles().add(userRole);
                },
                () -> log.warn(
                        "Rol por defecto '{}' no existe; el usuario {} se crea sin roles",
                        defaultRoleName, profile.email())
        );

        return userRepository.save(user);
    }

    private String resolveName(OAuthUserProfile profile) {
        String name = profile.name();
        if (name == null || name.isBlank()) {
            name = profile.email();
        }
        return name.length() > 100 ? name.substring(0, 100) : name;
    }
}

