package com.uc.ms_security.dto;

import com.uc.ms_security.dto.role.RoleResponseDTO;
import lombok.Value;

@Value
public class PermissionRoleResponseDTO {

    Long id;

    Long permissionId;

    RoleResponseDTO role;
}
