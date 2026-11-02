package com.uc.ms_security.dto.permission;

import lombok.Value;

@Value
public class RolePermissionResponseDTO {

    Long id;

    Long roleId;

    PermissionResponseDTO permission;
}
