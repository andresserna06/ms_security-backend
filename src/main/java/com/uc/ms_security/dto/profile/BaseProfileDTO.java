package com.uc.ms_security.dto.profile;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public abstract class BaseProfileDTO {

    @Size(
            max = 20,
            message = "El teléfono debe tener como máximo 20 caracteres"
    )
    private String phone;

    @Past(
            message = "La fecha de nacimiento debe ser una fecha pasada"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String photo;

    @Size(
            max = 100,
            message = "La ciudad no puede superar los 100 caracteres"
    )
    private String city;

    @Size(
            max = 500,
            message = "La descripción no puede superar los 500 caracteres"
    )
    private String bio;
}
