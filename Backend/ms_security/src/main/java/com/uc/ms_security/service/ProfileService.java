package com.uc.ms_security.service;

import com.uc.ms_security.dto.profile.CreateProfileDTO;
import com.uc.ms_security.dto.profile.ProfileResponseDTO;
import com.uc.ms_security.dto.profile.UpdateProfileDTO;
import com.uc.ms_security.entity.Profile;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.ProfileMapper;
import com.uc.ms_security.repository.ProfileRepository;
import com.uc.ms_security.repository.UserRepository; // Inyeccción de dependencias
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;

    public ProfileResponseDTO create(Long userId, CreateProfileDTO dto) {
        User user = userRepository.findById(userId) // Revisar si existe en la BD
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND, // Lanzamos nuestras excepciones personalizadas.
                        "Usuario no encontrado con ID: " + userId
                ));

        if (profileRepository.existsByUserId(userId)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El usuario ya cuenta con un perfil asignado"
            );
        }

        if (profileRepository.existsByPhone(dto.getPhone())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un perfil con este número de teléfono"
            );
        }

        Profile profile = profileMapper.toEntity(dto, user);
        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toResponseDTO(savedProfile);
    }

    public List<ProfileResponseDTO> findAll() {
        List<Profile> profiles = profileRepository.findAll();
        return profileMapper.toResponseDTOList(profiles);
    }

    private Profile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Perfil no encontrado para el usuario con id: " + userId
                ));
    }

    public ProfileResponseDTO findByUserId(Long userId) {
        return profileMapper.toResponseDTO(findProfile(userId));
    }

    public ProfileResponseDTO findById(Long id) {
        return profileMapper.toResponseDTO(profileRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Perfil no encontrado con id: " + id
                )));
    }

    public ProfileResponseDTO update(Long userId, UpdateProfileDTO dto) {
        Profile profile = findProfile(userId);

        if (profileRepository.existsByPhoneAndIdNot(dto.getPhone(), profile.getId())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El número de teléfono pertenece a otro perfil"
            );
        }

        profileMapper.updateEntity(dto, profile);
        Profile updatedProfile = profileRepository.save(profile);
        return profileMapper.toResponseDTO(updatedProfile);
    }

    public void delete(Long userId) {
        profileRepository.delete(findProfile(userId));
    }
}