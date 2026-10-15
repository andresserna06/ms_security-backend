package com.uc.ms_security.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BasePermissionDTO {

    @NotBlank(
            message = "La URL es obligatoria"
    )
    @Size(
            max = 255,
            message = "La URL no puede superar los 255 caracteres"
    )
    private String url;

    @NotBlank(
            message = "El método HTTP es obligatorio"
    )
    @Size(
            min = 3,
            max = 10,
            message = "El método HTTP debe tener entre 3 y 10 caracteres"
    )
    private String method;

    @NotBlank(
            message = "El modelo es obligatorio"
    )
    @Size(
            min = 2,
            max = 100,
            message = "El modelo debe tener entre 2 y 100 caracteres"
    )
    private String model;
}
