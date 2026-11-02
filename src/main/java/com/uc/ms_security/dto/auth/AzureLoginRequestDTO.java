package com.uc.ms_security.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureLoginRequestDTO {

    @NotBlank(message = "El token de Azure es obligatorio")
    private String idToken;
}
