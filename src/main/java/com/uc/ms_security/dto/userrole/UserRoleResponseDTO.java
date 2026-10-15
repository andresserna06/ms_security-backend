package com.uc.ms_security.dto.userrole;

import lombok.Value;
import com.uc.ms_security.dto.role.RoleResponseDTO;

@Value
public class UserRoleResponseDTO {

    Long id;

    Long userId;

    RoleResponseDTO role;
}
