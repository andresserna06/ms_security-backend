package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.entity.Profile;
import com.uc.ms_security.entity.Session;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.ProfileRepository;
import com.uc.ms_security.repository.SessionRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.service.oauth.OAuthProviderStrategy;
import com.uc.ms_security.service.oauth.OAuthUserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta la autenticación con proveedores externos (OAuth).
 * Depende únicamente de la abstracción OAuthProviderStrategy (DIP): Spring
 * inyecta todas las implementaciones disponibles y aquí se indexan por proveedor.
 */
@Service
public class OAuthService {

    private final Map<AuthProvider, OAuthProviderStrategy> strategies;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ProfileRepository profileRepository;
    private final JwtService jwtService;

    public OAuthService(
            List<OAuthProviderStrategy> providerStrategies,
            UserRepository userRepository,
            SessionRepository sessionRepository,
            ProfileRepository profileRepository,
            JwtService jwtService) {

        this.strategies = new EnumMap<>(AuthProvider.class);
        providerStrategies.forEach(strategy -> this.strategies.put(strategy.getProviderName(), strategy));

        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.profileRepository = profileRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDTO loginWithProvider(AuthProvider provider, String idToken) {
        OAuthProviderStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "Proveedor de autenticación no soportado: " + provider);
        }

        // 1. Validar identidad con el proveedor externo
        OAuthUserProfile profile = strategy.verifyAndExtractUser(idToken);

        // 2. Identificar o aprovisionar al usuario y sincronizar foto inicial si aplica
        User user = userRepository
                .findWithRolesAndPermissionsByEmail(profile.email())
                .map(existing -> validateAndSyncExistingUser(existing, provider, profile))
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

    private User validateAndSyncExistingUser(User user, AuthProvider provider, OAuthUserProfile profile) {
        validateExistingUser(user, provider);

        // Sincronización inicial de foto: Solo si el usuario NO tiene foto asignada en su perfil
        // Si el usuario ya personalizó o subió su propia foto, NUNCA se sobreescribe con la del proveedor.
        Profile userProfile = profileRepository.findByUserId(user.getId()).orElse(null);
        if (userProfile == null) {
            if (profile.photoUrl() != null && !profile.photoUrl().isBlank()) {
                Profile newProfile = new Profile();
                newProfile.setUser(user);
                newProfile.setPhoto(profile.photoUrl());
                profileRepository.save(newProfile);
                user.setProfile(newProfile);
            }
        } else if ((userProfile.getPhoto() == null || userProfile.getPhoto().isBlank())
                && profile.photoUrl() != null && !profile.photoUrl().isBlank()) {
            userProfile.setPhoto(profile.photoUrl());
            profileRepository.save(userProfile);
        }

        return user;
    }

    private User validateExistingUser(User user, AuthProvider provider) {
        // Criterio 11: correo registrado con autenticación básica -> rechazar
        if (user.getAuthProvider() == AuthProvider.LOCAL) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El correo ya fue registrado mediante autenticación directa. Inicie sesión con su contraseña.");
        }

        // Correo asociado a otro proveedor externo distinto
        if (user.getAuthProvider() != provider) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El correo ya está asociado a otro proveedor de autenticación: " + user.getAuthProvider());
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El usuario se encuentra inhabilitado");
        }

        // Criterios 12 y 13: acceso posterior, se reutiliza el usuario sin duplicar
        return user;
    }

    private User provisionUser(OAuthUserProfile profile, AuthProvider provider) {
        // Criterio 10: primer acceso, creación automática en tabla users
        User user = new User();
        user.setEmail(profile.email());
        user.setName(resolveName(profile));
        user.setPassword(null);
        user.setActive(true);
        user.setAuthProvider(provider);

        User savedUser = userRepository.save(user);

        // Asignación inicial de foto al registrarse por primera vez si el proveedor la incluye
        if (profile.photoUrl() != null && !profile.photoUrl().isBlank()) {
            Profile userProfile = new Profile();
            userProfile.setUser(savedUser);
            userProfile.setPhoto(profile.photoUrl());
            profileRepository.save(userProfile);
            savedUser.setProfile(userProfile);
        }

        return savedUser;
    }

    private String resolveName(OAuthUserProfile profile) {
        String name = profile.name();
        if (name == null || name.isBlank()) {
            name = profile.email();
        }
        return name.length() > 100 ? name.substring(0, 100) : name;
    }
}
