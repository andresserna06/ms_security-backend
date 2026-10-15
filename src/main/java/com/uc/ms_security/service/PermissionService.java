package com.uc.ms_security.service;

import com.uc.ms_security.dto.permission.CreatePermissionDTO;
import com.uc.ms_security.dto.permission.PermissionResponseDTO;
import com.uc.ms_security.dto.permission.UpdatePermissionDTO;
import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.mapper.PermissionMapper;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionResponseDTO create(CreatePermissionDTO dto) {
        String method = dto.getMethod().toUpperCase();
        if (permissionRepository.existsByUrlAndMethod(dto.getUrl(), method)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un permiso para la URL '" + dto.getUrl() + "' y el método '" + method + "'"
            );
        }

        Permission permission = permissionMapper.toEntity(dto);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponseDTO(savedPermission);
    }

    public List<PermissionResponseDTO> findAll() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissionMapper.toResponseDTOList(permissions);
    }

    private Permission findPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Permiso no encontrado con id: " + id
                ));
    }

    public PermissionResponseDTO findById(Long id) {
        Permission permission = findPermission(id);
        return permissionMapper.toResponseDTO(permission);
    }


    public PermissionResponseDTO update(Long id, UpdatePermissionDTO dto) {
        Permission permission = findPermission(id);
        String method = dto.getMethod().toUpperCase();

        if (permissionRepository.existsByUrlAndMethodAndIdNot(dto.getUrl(), method, id)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "La combinación de URL y método HTTP ya pertenece a otro permiso"
            );
        }

        permissionMapper.updateEntity(dto, permission);
        Permission updatedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponseDTO(updatedPermission);
    }

    public void delete(Long id) {
        Permission permission = findPermission(id);
        permissionRepository.delete(permission);
    }
}
