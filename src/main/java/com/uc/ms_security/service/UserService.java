package com.uc.ms_security.service;

import com.uc.ms_security.dto.user.CreateUserDTO;
import com.uc.ms_security.dto.user.UpdateUserDTO;
import com.uc.ms_security.dto.user.UserDetailResponseDTO;
import com.uc.ms_security.dto.user.UserResponseDTO;
import com.uc.ms_security.dto.user.UserRolesResponseDTO;
import com.uc.ms_security.dto.user.UserSelectorResponseDTO;
import com.uc.ms_security.dto.user.UserSessionsResponseDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.UserMapper;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Value("${security.oauth.default-role:USER}")
    private String defaultUserRoleName;

    @Transactional
    public UserResponseDTO create(CreateUserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un usuario con este email"
            );
        }
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        User user = userMapper.toEntity(dto, encodedPassword);
        User savedUser = userRepository.save(user);
        assignDefaultRole(savedUser);
        return userMapper.toResponseDTO(savedUser);
    }

    private void assignDefaultRole(User user) {
        String roleName = resolveDefaultUserRoleName();
        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.INVALID_OPERATION,
                        "No existe el rol por defecto configurado: " + roleName
                ));

        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            return;
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
    }

    private String resolveDefaultUserRoleName() {
        if (defaultUserRoleName == null || defaultUserRoleName.isBlank()) {
            return "USER";
        }
        return defaultUserRoleName.trim().toUpperCase(Locale.ROOT);
    }

    public List<UserDetailResponseDTO> findAll() {
        List<User> users = userRepository.findAll();
        return userMapper.toDetailResponseDTOList(users);
    }

    @Transactional(readOnly = true)
    public List<UserSelectorResponseDTO> findAllForSelector() {
        return userRepository.findAllByActiveTrue().stream()
                .map(user -> new UserSelectorResponseDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getActive()))
                .toList();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Usuario no encontrado con id: " + id
                ));
    }

    public UserResponseDTO findById(Long id) {
        User user = findUser(id);
        return userMapper.toResponseDTO(user);
    }

    public UserDetailResponseDTO findByIdWithProfile(Long id) {
        User user = userRepository.findWithProfileById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Usuario no encontrado con id: " + id
                ));
        return userMapper.toDetailResponseDTO(user);
    }

    // Buscar las sesiones de un usuario
    public UserSessionsResponseDTO findByIdAndSessions(Long id) {
        User user = userRepository
                .findWithSessionsById(id)
                .orElseThrow(
                        () -> new ApplicationException(
                                ErrorCase.NOT_FOUND,
                                "Usuario no encontrado con id: " + id
                        )
                );

        return userMapper.toSessionsResponseDTO(user);
    }

    public UserResponseDTO update(Long id, UpdateUserDTO dto) {
        User user = findUser(id);
        if (userRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El email pertenece a otro usuario"
            );
        }
        userMapper.updateBasicData(dto, user);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
    }

    public UserResponseDTO deactivate(Long id) {
        User user = findUser(id);
        user.setActive(false);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
    }

    public UserResponseDTO activate(Long id) {
        User user = findUser(id);
        user.setActive(true);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
    }

    public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
    }

    public UserRolesResponseDTO findByIdAndRoles(Long id) {
        User user = userRepository
                .findWithRolesById(id)
                .orElseThrow(
                        () -> new ApplicationException(
                                ErrorCase.NOT_FOUND,
                                "Usuario no encontrado con id: " + id
                        )
                );

        return userMapper.toRolesResponseDTO(user);
    }
}
