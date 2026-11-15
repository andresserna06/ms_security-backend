package com.uc.ms_security.authorization;

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
public class DynamicAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

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

        try {
            Long.parseLong(jwtAuthentication.getName());
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

        Object permissionsClaim =
                jwtAuthentication.getToken().getClaims().get("permissions");
        if (!(permissionsClaim instanceof Collection<?> permissions)) {
            return new AuthorizationDecision(false);
        }

        PathContainer requestPath;
        try {
            requestPath = PathContainer.parsePath(requestUrl);
        } catch (IllegalArgumentException exception) {
            return new AuthorizationDecision(false);
        }
        for (Object permission : permissions) {
            if (permission instanceof String permissionValue
                    && matches(permissionValue, requestMethod, requestPath)) {
                return new AuthorizationDecision(true);
            }
        }

        return new AuthorizationDecision(false);
    }

    private boolean matches(
            String permission,
            String requestMethod,
            PathContainer requestPath) {
        int separator = permission.indexOf(':');
        if (separator <= 0 || separator == permission.length() - 1
                || !permission.substring(0, separator).equalsIgnoreCase(requestMethod)) {
            return false;
        }

        String permissionPath = permission.substring(separator + 1);
        try {
            PathPattern pattern = pathPatternParser.parse(permissionPath);
            return pattern.matches(requestPath);
        } catch (PatternParseException exception) {
            return false;
        }
    }
}
