package com.uc.ms_security.dto.profile;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public abstract class BaseProfileDTO { // ProfileRequestDTO

    @NotBlank(
            message = "El teléfono es obligatorio"
    )
    @Size(
            min = 7,
            max = 20,
            message = "El teléfono debe tener entre 7 y 20 caracteres"
    )
    private String phone;

    @NotNull(
            message = "La fecha de nacimiento es obligatoria"
    )
    @Past(
            message = "La fecha de nacimiento debe ser una fecha pasada"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}
