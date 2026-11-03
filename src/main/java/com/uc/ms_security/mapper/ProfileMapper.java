package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.profile.CreateProfileDTO;
import com.uc.ms_security.dto.profile.ProfileResponseDTO;
import com.uc.ms_security.dto.profile.UpdateProfileDTO;
import com.uc.ms_security.entity.Profile;
import com.uc.ms_security.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProfileMapper {

    public Profile toEntity(CreateProfileDTO dto, User user) {
        Profile profile = new Profile();

        profile.setPhone(normalizeString(dto.getPhone()));
        profile.setBirthDate(dto.getBirthDate());
        profile.setPhoto(normalizeString(dto.getPhoto()));
        profile.setCity(normalizeString(dto.getCity()));
        profile.setBio(normalizeString(dto.getBio()));
        profile.setUser(user);

        return profile;
    }

    public void updateEntity(UpdateProfileDTO dto, Profile profile) {
        profile.setPhone(normalizeString(dto.getPhone()));
        profile.setBirthDate(dto.getBirthDate());
        profile.setPhoto(normalizeString(dto.getPhoto()));
        profile.setCity(normalizeString(dto.getCity()));
        profile.setBio(normalizeString(dto.getBio()));
    }

    public ProfileResponseDTO toResponseDTO(Profile profile) {
        if (profile == null) {
            return null;
        }

        return new ProfileResponseDTO(
                profile.getId(),
                profile.getPhone(),
                profile.getBirthDate(),
                profile.getPhoto(),
                profile.getCity(),
                profile.getBio(),
                profile.getUser() != null ? profile.getUser().getId() : null
        );
    }

    public List<ProfileResponseDTO> toResponseDTOList(List<Profile> profiles) {
        return profiles.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private String normalizeString(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        return val.trim();
    }
}
