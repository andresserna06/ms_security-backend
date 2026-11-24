package com.uc.ms_security.controller;

import com.uc.ms_security.dto.userrole.AssignRoleRequestDTO;
import com.uc.ms_security.dto.userrole.RoleUserResponseDTO;
import com.uc.ms_security.dto.userrole.UserRoleResponseDTO;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.service.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserRoleResponseDTO assign(
            @Valid @RequestBody AssignRoleRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt) {

        return userRoleService.assign(
                dto,
                currentRoles(jwt)
        );
    }

    @GetMapping
    public List<UserRoleResponseDTO> findAll() {
        return userRoleService.findAll();
    }

    @GetMapping("/users/{userId}")
    public List<UserRoleResponseDTO> findByUserId(
            @PathVariable Long userId) {

        return userRoleService.findByUserId(userId);
    }

    @GetMapping("/roles/{roleId}")
    public List<RoleUserResponseDTO> findByRoleId(
            @PathVariable Long roleId) {

        return userRoleService.findByRoleId(roleId);
    }

    @DeleteMapping("/{userId}/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            @AuthenticationPrincipal Jwt jwt) {

        userRoleService.delete(
                userId,
                roleId,
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
