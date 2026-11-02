
package com.uc.ms_security.service;

import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionAuthorizationService {

    private final PermissionRepository permissionRepository;

    private final PathPatternParser pathPatternParser =
            new PathPatternParser();

    public boolean hasPermission(
            Long userId,
            String requestUrl,
            String requestMethod
    ) {
        if (userId == null || requestUrl == null || requestMethod == null) {
            return false;
        }

        List<Permission> permissions =
                permissionRepository.findAllByUserId(userId);

        PathContainer requestPath =
                PathContainer.parsePath(requestUrl);

        for (Permission permission : permissions) {

            if (permission.getUrl() == null
                    || permission.getMethod() == null) {
                continue;
            }

            if (!permission.getMethod().equalsIgnoreCase(requestMethod)) {
                continue;
            }

            try {
                PathPattern pattern =
                        pathPatternParser.parse(permission.getUrl());

                if (pattern.matches(requestPath)) {
                    return true;
                }

            } catch (PatternParseException exception) {
                // Un patrón inválido no concede autorización.
                continue;
            }
        }

        return false;
    }
}
