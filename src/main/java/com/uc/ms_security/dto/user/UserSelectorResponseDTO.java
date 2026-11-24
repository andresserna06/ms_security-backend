package com.uc.ms_security.dto.user;

public record UserSelectorResponseDTO(
        Long id,
        String name,
        String email,
        Boolean active
) {
}
