package com.uc.ms_security.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GithubLoginRequestDTO {

    @NotBlank(message = "El código de autorización de GitHub es obligatorio")
    private String code;
}
