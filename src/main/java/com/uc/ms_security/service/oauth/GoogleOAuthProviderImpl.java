package com.uc.ms_security.service.oauth;

import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Implementación concreta (detalle) de OAuthProviderStrategy para Google.
 * Es el único punto del sistema que conoce cómo validar un idToken de Google:
 * firma RS256 con las llaves públicas de Google, expiración, emisor, audiencia y email verificado.
 */
@Component
public class GoogleOAuthProviderImpl implements OAuthProviderStrategy {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthProviderImpl.class);
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
            "accounts.google.com",
            "https://accounts.google.com"
    );

    private final String clientId;
    private final NimbusJwtDecoder googleJwtDecoder;

    public GoogleOAuthProviderImpl(
            @Value("${security.oauth.google.client-id:}") String clientId) {
        this.clientId = clientId;
        this.googleJwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(GOOGLE_JWK_SET_URI)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        this.googleJwtDecoder.setJwtValidator(buildValidator());
    }

    @Override
    public AuthProvider getProviderName() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserProfile verifyAndExtractUser(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            log.error("No se ha configurado security.oauth.google.client-id (GOOGLE_CLIENT_ID)");
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "El inicio de sesión con Google no está configurado en el servidor"
            );
        }

        Jwt jwt;
        try {
            jwt = googleJwtDecoder.decode(idToken);
        } catch (JwtException ex) {
            log.warn("idToken de Google rechazado: {}", ex.getMessage());
            throw invalidToken();
        }

        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        if (email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "La cuenta de Google no tiene un correo verificado"
            );
        }

        return new OAuthUserProfile(
                email.trim().toLowerCase(),
                jwt.getClaimAsString("name")
        );
    }

    private OAuth2TokenValidator<Jwt> buildValidator() {
        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            return issuer != null && GOOGLE_ISSUERS.contains(issuer)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "Emisor del token no válido", null));
        };

        OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Audiencia del token no válida", null));

        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                issuerValidator,
                audienceValidator
        );
    }

    private ApplicationException invalidToken() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "Token de Google inválido o expirado"
        );
    }
}

