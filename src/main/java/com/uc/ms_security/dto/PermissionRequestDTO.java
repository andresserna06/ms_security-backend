package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionRequestDTO {

    @NotBlank(message = "La url es obligatoria")
    @Size(
            max = 255,
            message = "La url no puede superar 255 caracteres"
    )
    private String url;

    @NotBlank(message = "El método es obligatorio")
    @Size(
            max = 10,
            message = "El método no puede superar 10 caracteres"
    )
    private String method;

    @NotBlank(message = "El módulo es obligatorio")
    @Size(
            max = 80,
            message = "El módulo no puede superar 80 caracteres"
    )
    private String module;
}
