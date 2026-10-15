package com.uc.ms_security.dto.profile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileDTO extends BaseProfileDTO {
    // Hereda phone y birthDate de BaseProfileDTO.
    // El userId no es modificable para preservar la integridad de la relación 1 a 1.
}
