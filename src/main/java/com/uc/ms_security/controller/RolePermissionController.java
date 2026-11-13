package com.uc.ms_security.controller;

import com.uc.ms_security.dto.permission.AssignPermissionRequestDTO;
import com.uc.ms_security.dto.permission.PermissionRoleResponseDTO;
import com.uc.ms_security.dto.permission.RolePermissionResponseDTO;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.service.RolePermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolePermissionResponseDTO assign(
            @Valid @RequestBody AssignPermissionRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        return rolePermissionService.assign(
                dto,
                currentUserId(jwt),
                currentRequestUrl(request),
                request.getMethod()
        );
    }

    @GetMapping
    public List<RolePermissionResponseDTO> findAll() {
        return rolePermissionService.findAll();
    }

    @GetMapping("/roles/{roleId}")
    public List<RolePermissionResponseDTO> findByRoleId(
            @PathVariable Long roleId) {

        return rolePermissionService.findByRoleId(roleId);
    }

    @GetMapping("/permissions/{permissionId}")
    public List<PermissionRoleResponseDTO> findByPermissionId(
            @PathVariable Long permissionId) {

        return rolePermissionService.findByPermissionId(permissionId);
    }

    @DeleteMapping("/{roleId}/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long roleId,
            @PathVariable Long permissionId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        rolePermissionService.delete(
                roleId,
                permissionId,
                currentUserId(jwt),
                currentRequestUrl(request),
                request.getMethod()
        );
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ApplicationException(
                    ErrorCase.FORBIDDEN,
                    "No tienes permisos para modificar este rol"
            );
        }

        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ApplicationException(
                    ErrorCase.FORBIDDEN,
                    "No tienes permisos para modificar este rol"
            );
        }
    }

    private String currentRequestUrl(HttpServletRequest request) {
        return request.getRequestURI()
                .substring(request.getContextPath().length());
    }
}
