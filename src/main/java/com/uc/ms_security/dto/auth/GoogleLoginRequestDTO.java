package com.uc.ms_security.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequestDTO {

    @NotBlank(message = "El token de Google es obligatorio")
    private String idToken;
}

