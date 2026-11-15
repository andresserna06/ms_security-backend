package com.uc.ms_security.service;

import com.uc.ms_security.exception.ApplicationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RolePermissionAuthorizationServiceTest {

    private final RolePermissionAuthorizationService authorizationService =
            new RolePermissionAuthorizationService();

    @Test
    void allowsAdminRoleFromToken() {
        assertDoesNotThrow(() ->
                authorizationService.ensureCanManage(List.of("ADMIN")));
    }

    @Test
    void allowsRoleAdminClaimFromToken() {
        assertDoesNotThrow(() ->
                authorizationService.ensureCanManage(List.of("ROLE_ADMIN")));
    }

    @Test
    void deniesNonAdminEvenWhenRolesArePresent() {
        assertThrows(
                ApplicationException.class,
                () -> authorizationService.ensureCanManage(
                        List.of("Jugador", "Consultor")
                )
        );
    }
}
