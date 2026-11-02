package com.uc.ms_security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.uc.ms_security.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Configuration // Esta anotación le dice a Spring que lea este archivo apenas encienda 
public class JwtConfig {

    @Bean // toma el objeto que devuelva y guárdalo en tu caja de herramientas para que el resto del sistema lo pueda usar.
    public SecretKey jwtSecretKey(
            @Value("${security.jwt.secret}") String secret) {

        byte[] secretBytes = secret.getBytes(
                StandardCharsets.UTF_8
        );

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "La clave JWT debe tener al menos 32 bytes"
            );
        }

        return new SecretKeySpec(
                secretBytes,
                "HmacSHA256"
        );
    }

    @Bean
    public JwtEncoder jwtEncoder( // Encargado de crear los tokens
            SecretKey jwtSecretKey) { // Recibe el sello o la clave definida en jwtSecretKey

        OctetSequenceKey jwk = // La libreria de seguridad Nimbus exige que la llave este empaquetada en formato jwk (JSON Web Key)
                new OctetSequenceKey.Builder(
                        jwtSecretKey.getEncoded() // Estas líneas de código simplemente toman tu llave y la envuelven en el envoltorio exacto que pide la librería
                ).build();

        JWKSource<SecurityContext> jwkSource =
                new ImmutableJWKSet<>(
                        new JWKSet(jwk)
                );

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder( // Validador de tokens
            SecretKey jwtSecretKey,
            SessionRepository sessionRepository) { 

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey) // Para verificar las firmas, vas a usar esta llave y vas a aplicar el algoritmo matemático HS256
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> issuerAndTimestampValidator =
                JwtValidators.createDefaultWithIssuer("ms-security");

        // Valida que la sesión asociada al token exista en BD y no haya sido cerrada (logout) ni expirada
        OAuth2TokenValidator<Jwt> activeSessionValidator = jwt ->
                sessionRepository.findByToken(jwt.getTokenValue())
                        .filter(session -> session.getExpiration() != null
                                && session.getExpiration().isAfter(LocalDateTime.now()))
                        .map(session -> OAuth2TokenValidatorResult.success())
                        .orElseGet(() -> OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "La sesión ha expirado o fue cerrada", null)
                        ));

        decoder.setJwtValidator( // esta línea añade una capa extra de seguridad: le ordena revisar que el emisor (Issuer) del token sea obligatoriamente "ms-security" y que la sesión siga activa en BD
                new DelegatingOAuth2TokenValidator<>(
                        issuerAndTimestampValidator,
                        activeSessionValidator
                )
        );

        return decoder; // A partir de ahora, Spring Security lo usará en la puerta de todas tus rutas protegidas
    }

    /*Al crear el token: Cuando el usuario inicia sesión exitosamente, el creador de tokens (JwtService)
    arma el pase y le estampa un sello que dice: "Emitido por: ms-security".  
    Al validar el token: El guardia de seguridad (JwtDecoder) tiene la orden 
    estricta de revisar ese texto específico. Si recibe un pase válido
    que no está vencido y tiene la firma matemática correcta,
    pero su texto dice "Emitido por: ms-ventas" o "Emitido por: google", 
    lo va a rechazar de inmediato.   Esta es una medida de seguridad vital.
    Sirve para que, si en el futuro tu empresa tiene varias aplicaciones distintas (microservicios),
    los tokens generados para una aplicación no puedan usarse para infiltrarse en otra, incluso si comparten algunas configuraciones
     */
}
