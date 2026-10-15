package com.uc.ms_security.dto.user;

import com.uc.ms_security.dto.userrole.UserRoleResponseDTO;
import lombok.Value;

import java.util.List;

@Value
public class UserRolesResponseDTO {

    Long id;

    String name;

    String email;

    List<UserRoleResponseDTO> roles;
}
