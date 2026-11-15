package com.uc.ms_security.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicAuthorizationManagerTest {

    private final DynamicAuthorizationManager manager =
            new DynamicAuthorizationManager();

    @Test
    void grantsPermissionForMatchingMethodAndVariablePathWithContextPath() {
        JwtAuthenticationToken authentication = jwtAuthentication(
                List.of("POST:/api/roles/{roleId}")
        );

        assertTrue(authorize(
                authentication,
                "POST",
                "/security/api/roles/24",
                "/security"
        ).isGranted());
    }

    @Test
    void grantsCreateRoleWhenPermissionIsPresentInToken() {
        JwtAuthenticationToken authentication = jwtAuthentication(
                List.of("POST:/api/roles")
        );

        assertTrue(authorize(
                authentication,
                "POST",
                "/api/roles",
                ""
        ).isGranted());
    }

    @Test
    void deniesWhenMethodDoesNotMatch() {
        JwtAuthenticationToken authentication = jwtAuthentication(
                List.of("POST:/api/roles")
        );

        assertFalse(authorize(
                authentication,
                "GET",
                "/api/roles",
                ""
        ).isGranted());
    }

    @Test
    void deniesWhenPermissionsClaimIsMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("42")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("roles", List.of("ADMIN"))
                .build();

        assertFalse(authorize(
                new JwtAuthenticationToken(jwt, List.of()),
                "POST",
                "/api/roles",
                ""
        ).isGranted());
    }

    @Test
    void deniesAuthenticatedNonJwtAuthentication() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "user",
                        "password",
                        List.of()
                );

        assertFalse(authorize(
                authentication,
                "POST",
                "/api/roles",
                ""
        ).isGranted());
    }

    private AuthorizationResult authorize(
            Authentication authentication,
            String method,
            String requestUri,
            String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(requestUri);
        request.setContextPath(contextPath);

        return manager.authorize(
                () -> authentication,
                new RequestAuthorizationContext(request)
        );
    }

    private JwtAuthenticationToken jwtAuthentication(List<String> permissions) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("42")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("roles", List.of("Jugador"))
                .claim("permissions", permissions)
                .build();

        return new JwtAuthenticationToken(jwt, List.of());
    }
}
