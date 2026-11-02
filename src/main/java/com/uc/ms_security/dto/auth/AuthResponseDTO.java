package com.uc.ms_security.dto.auth;

import java.time.Instant;

public record AuthResponseDTO( // Un record es como un diccionario
        String accessToken,
        String tokenType,
        Instant expiresAt) {
}

