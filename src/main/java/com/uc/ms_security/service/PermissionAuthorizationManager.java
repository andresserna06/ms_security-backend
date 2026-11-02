package com.uc.ms_security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PermissionAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final PermissionAuthorizationService authorizationService;

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        Authentication currentAuthentication = authentication.get();

        // Solo aceptamos usuarios autenticados mediante JWT
        if (!(currentAuthentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !currentAuthentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        // El subject del JWT contiene el ID del usuario
        Long userId;

        try {
            userId = Long.parseLong(jwtAuthentication.getName());
        } catch (NumberFormatException exception) {
            return new AuthorizationDecision(false);
        }

        // Obtenemos la ruta y el método de la petición
        String requestUrl = context.getRequest().getRequestURI()
                .substring(context.getRequest().getContextPath().length());

        String requestMethod = context.getRequest().getMethod();

        // Consultamos los permisos actuales en MySQL
        boolean allowed = authorizationService.hasPermission(
                userId,
                requestUrl,
                requestMethod);

        return new AuthorizationDecision(allowed);
    }
}