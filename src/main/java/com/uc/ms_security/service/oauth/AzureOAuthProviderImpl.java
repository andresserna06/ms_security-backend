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

/**
 * Implementación concreta (detalle) de OAuthProviderStrategy para Microsoft Azure (Entra ID).
 * Valida el idToken emitido por Microsoft:
 * firma RS256 con las llaves públicas de Microsoft (JWKS), expiración, emisor y audiencia.
 */
@Component
public class AzureOAuthProviderImpl implements OAuthProviderStrategy {

    private static final Logger log = LoggerFactory.getLogger(AzureOAuthProviderImpl.class);
    private static final String AZURE_JWK_SET_URI = "https://login.microsoftonline.com/common/discovery/v2.0/keys";

    private final String clientId;
    private final NimbusJwtDecoder azureJwtDecoder;

    public AzureOAuthProviderImpl(
            @Value("${security.oauth.azure.client-id:}") String clientId) {
        this.clientId = clientId;
        this.azureJwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(AZURE_JWK_SET_URI)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        this.azureJwtDecoder.setJwtValidator(buildValidator());
    }

    @Override
    public AuthProvider getProviderName() {
        return AuthProvider.AZURE;
    }

    @Override
    public OAuthUserProfile verifyAndExtractUser(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            log.error("No se ha configurado security.oauth.azure.client-id (AZURE_CLIENT_ID)");
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "El inicio de sesión con Microsoft Azure no está configurado en el servidor"
            );
        }

        Jwt jwt;
        try {
            jwt = azureJwtDecoder.decode(idToken);
        } catch (JwtException ex) {
            log.warn("idToken de Azure rechazado: {}", ex.getMessage());
            throw invalidToken();
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("upn");
        }

        if (email == null || email.isBlank()) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "La cuenta de Microsoft Azure no contiene un correo electrónico identificable"
            );
        }

        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = email;
        }

        return new OAuthUserProfile(
                email.trim().toLowerCase(),
                name,
                jwt.getClaimAsString("picture")
        );
    }

    private OAuth2TokenValidator<Jwt> buildValidator() {
        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            boolean valid = issuer != null && (
                    issuer.startsWith("https://login.microsoftonline.com/") ||
                    issuer.startsWith("https://sts.windows.net/")
            );
            return valid
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "Emisor del token de Azure no válido", null));
        };

        OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Audiencia del token de Azure no válida", null));

        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                issuerValidator,
                audienceValidator
        );
    }

    private ApplicationException invalidToken() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "Token de Microsoft Azure inválido o expirado"
        );
    }
}
