package com.uc.ms_security.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Verify2FaRequestDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "El código 2FA es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código 2FA debe ser de 6 dígitos numéricos")
    private String code;
}
