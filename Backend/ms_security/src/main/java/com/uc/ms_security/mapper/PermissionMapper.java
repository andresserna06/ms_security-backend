package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.permission.CreatePermissionDTO;
import com.uc.ms_security.dto.permission.PermissionResponseDTO;
import com.uc.ms_security.dto.permission.UpdatePermissionDTO;
import com.uc.ms_security.entity.Permission;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PermissionMapper {

    public Permission toEntity(CreatePermissionDTO dto) {
        Permission permission = new Permission();

        permission.setUrl(dto.getUrl());
        permission.setMethod(dto.getMethod().toUpperCase());
        permission.setModel(dto.getModel());

        return permission;
    }

    public void updateEntity(UpdatePermissionDTO dto, Permission permission) {
        permission.setUrl(dto.getUrl());
        permission.setMethod(dto.getMethod().toUpperCase());
        permission.setModel(dto.getModel());
    }

    public PermissionResponseDTO toResponseDTO(Permission permission) {
        return new PermissionResponseDTO(
                permission.getId(),
                permission.getUrl(),
                permission.getMethod(),
                permission.getModel()
        );
    }

    public List<PermissionResponseDTO> toResponseDTOList(List<Permission> permissions) {
        return permissions.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
