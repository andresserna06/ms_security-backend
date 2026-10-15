package com.uc.ms_security.dto.user;

import com.uc.ms_security.dto.profile.ProfileResponseDTO;
import lombok.Value;

@Value
public class UserDetailResponseDTO { // Detallado "Detail" - Se refiere a que va a traer las relaciones 

    Long id;

    String name;

    String email;

    ProfileResponseDTO profile;
}
