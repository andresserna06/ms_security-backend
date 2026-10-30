package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService { // JwtService define la información y duración del token, y solicita a JwtEncoder que lo firme.

    private static final Duration TOKEN_DURATION =
            Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder; // inyeccion de dependencias

    public AuthResponseDTO generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_DURATION);

        List<String> roles = user.getUserRoles() != null
                ? user.getUserRoles().stream()
                        .map(ur -> ur.getRole().getName())
                        .distinct()
                        .toList()
                : List.of();

        List<String> permissions = user.getUserRoles() != null
                ? user.getUserRoles().stream()
                        .map(ur -> ur.getRole())
                        .filter(role -> role != null && role.getRolePermissions() != null)
                        .flatMap(role -> role.getRolePermissions().stream())
                        .filter(rp -> rp != null && rp.getPermission() != null)
                        .map(rp -> rp.getPermission().getMethod() + ":" + rp.getPermission().getUrl())
                        .distinct()
                        .toList()
                : List.<String>of();

        JwtClaimsSet claims = JwtClaimsSet.builder() // Los claims son la información pública que viajará dentro del token
                .issuer("ms-security") // Emisor
                .subject(user.getId().toString()) // sub Id estable del usuario
                .issuedAt(now) // iat momento de creación
                .expiresAt(expiresAt) // momento de expiración
                .id(UUID.randomUUID().toString()) // jti identificador unico del token
                .claim("email", user.getEmail()) // dato informativo email
                .claim("roles", roles)
                .claim("permissions", permissions)
                .build();

// Nunca debemos incluir la contraseña en el JWT.

        JwsHeader header = JwsHeader // Se indica que el token usará el algoritmo matemático HS256. Básicamente, le dice al sistema qué fórmula usar al momento de aplicar la firma
                .with(MacAlgorithm.HS256)
                .build();

        Jwt jwt = jwtEncoder.encode(  
                JwtEncoderParameters.from( // En este punto, el JwtEncoder toma los datos visibles (Claims) y las instrucciones (Header), los mezcla con tu llave secreta oculta y genera el token real con su firma criptográfica
                        header, 
                        claims
                )
        );

        return new AuthResponseDTO( // Se genera la respuesta de autenticación, con su token, la palabra bearer y la expiracion listo para ser enviado al cliente
                jwt.getTokenValue(),
                "Bearer",
                expiresAt
        );
    }
}