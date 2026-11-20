package com.uc.ms_security.authorization;

import com.uc.ms_security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.PathContainer;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import java.util.Collection;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class DynamicAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final PermissionService permissionService;
    private final PathPatternParser pathPatternParser = new PathPatternParser();

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        Authentication currentAuthentication = authentication.get();

        if (!(currentAuthentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !currentAuthentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        Long userId;

        try {
            userId = Long.parseLong(jwtAuthentication.getName());
        } catch (NumberFormatException exception) {
            return new AuthorizationDecision(false);
        }

        String contextPath = context.getRequest().getContextPath();
        String requestUri = context.getRequest().getRequestURI();
        if (contextPath == null || requestUri == null
                || !requestUri.startsWith(contextPath)) {
            return new AuthorizationDecision(false);
        }

        String requestUrl = requestUri.substring(contextPath.length());
        String requestMethod = context.getRequest().getMethod();

        boolean tokenAllowsRequest = hasTokenPermission(
                jwtAuthentication,
                requestMethod,
                requestUrl);
        if (!tokenAllowsRequest) {
            return new AuthorizationDecision(false);
        }

        boolean roleAllowsRequest = permissionService.hasPermission(
                userId,
                requestMethod,
                requestUrl);

        return new AuthorizationDecision(roleAllowsRequest);
    }

    private boolean hasTokenPermission(
            JwtAuthenticationToken authentication,
            String requestMethod,
            String requestUrl) {
        Object permissionsClaim =
                authentication.getToken().getClaims().get("permissions");
        if (!(permissionsClaim instanceof Collection<?> permissions)) {
            return false;
        }

        PathContainer requestPath;
        try {
            requestPath = PathContainer.parsePath(requestUrl);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        for (Object permission : permissions) {
            if (!(permission instanceof String permissionValue)) {
                continue;
            }

            int separator = permissionValue.indexOf(':');
            if (separator <= 0 || separator == permissionValue.length() - 1
                    || !permissionValue.substring(0, separator)
                            .equalsIgnoreCase(requestMethod)) {
                continue;
            }

            try {
                PathPattern pattern =
                        pathPatternParser.parse(permissionValue.substring(separator + 1));
                if (pattern.matches(requestPath)) {
                    return true;
                }
            } catch (PatternParseException exception) {
                continue;
            }
        }

        return false;
    }
}
