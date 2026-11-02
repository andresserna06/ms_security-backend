package com.uc.ms_security.service;

import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

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
        List<Permission> permissions =
                permissionRepository.findAllByUserId(userId);

        return permissions.stream().anyMatch(permission -> {

            if (!permission.getMethod().equalsIgnoreCase(requestMethod)) {
                return false;
            }

            PathPattern pattern = pathPatternParser.parse(
                    permission.getUrl()
            );

            return pattern.matches(
                    org.springframework.http.server
                            .PathContainer.parsePath(requestUrl)
            );
        });
    }
}