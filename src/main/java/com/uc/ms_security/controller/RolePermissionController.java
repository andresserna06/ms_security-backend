package com.uc.ms_security.controller;

import com.uc.ms_security.dto.permission.AssignPermissionRequestDTO;
import com.uc.ms_security.dto.permission.PermissionRoleResponseDTO;
import com.uc.ms_security.dto.permission.RolePermissionResponseDTO;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
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
            @AuthenticationPrincipal Jwt jwt) {

        return rolePermissionService.assign(
                dto,
                currentRoles(jwt)
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
            @AuthenticationPrincipal Jwt jwt) {

        rolePermissionService.delete(
                roleId,
                permissionId,
                currentRoles(jwt)
        );
    }

    private Collection<String> currentRoles(Jwt jwt) {
        if (jwt == null) {
            throw new ApplicationException(
                    ErrorCase.FORBIDDEN,
                    "No tienes permisos para modificar este rol"
            );
        }

        Object rolesClaim = jwt.getClaims().get("roles");
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
}
