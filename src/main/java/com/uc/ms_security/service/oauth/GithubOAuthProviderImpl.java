package com.uc.ms_security.service.oauth;

import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Implementación concreta (detalle) de OAuthProviderStrategy para GitHub.
 * GitHub no emite un idToken: recibe un authorization code y lo canjea por un
 * access token usando el client secret, que nunca sale del backend.
 */
@Component
public class GithubOAuthProviderImpl implements OAuthProviderStrategy {

    private static final Logger log = LoggerFactory.getLogger(GithubOAuthProviderImpl.class);
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";
    private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";
    private static final String GITHUB_ACCEPT = "application/vnd.github+json";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestClient restClient;

    public GithubOAuthProviderImpl(
            @Value("${security.oauth.github.client-id:}") String clientId,
            @Value("${security.oauth.github.client-secret:}") String clientSecret,
            @Value("${security.oauth.github.redirect-uri:}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public AuthProvider getProviderName() {
        return AuthProvider.GITHUB;
    }

    @Override
    public OAuthUserProfile verifyAndExtractUser(String code) {
        validateConfiguration();

        String accessToken = exchangeCodeForAccessToken(code);
        Map<String, Object> githubUser = fetchGithubUser(accessToken);
        String email = extractEmail(accessToken, githubUser);
        String name = extractName(githubUser, email);

        String photoUrl = null;
        Object avatar = githubUser.get("avatar_url");
        if (avatar instanceof String avatarStr && !avatarStr.isBlank()) {
            photoUrl = avatarStr.trim();
        }

        return new OAuthUserProfile(email, name, photoUrl);
    }

    private void validateConfiguration() {
        if (isBlank(clientId) || isBlank(clientSecret) || isBlank(redirectUri)) {
            log.error("Faltan variables de configuración de GitHub OAuth (client-id, client-secret o redirect-uri)");
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "El inicio de sesión con GitHub no está configurado en el servidor"
            );
        }
    }

    private String exchangeCodeForAccessToken(String code) {
        if (isBlank(code)) {
            throw invalidCode();
        }

        String formBody = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(redirectUri);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(GITHUB_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.USER_AGENT, "ms-security")
                    .body(formBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("access_token") == null) {
                log.warn("GitHub rechazó el código de autorización: {}", response);
                throw invalidCode();
            }

            return response.get("access_token").toString();
        } catch (RestClientException ex) {
            log.error("Error al canjear el código de GitHub: {}", ex.getMessage(), ex);
            throw invalidCode();
        }
    }

    private Map<String, Object> fetchGithubUser(String accessToken) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> user = restClient.get()
                    .uri(GITHUB_USER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.USER_AGENT, "ms-security")
                    .header(HttpHeaders.ACCEPT, GITHUB_ACCEPT)
                    .retrieve()
                    .body(Map.class);

            if (user == null) {
                throw invalidCode();
            }

            return user;
        } catch (RestClientException ex) {
            log.error("Error al consultar el perfil de GitHub: {}", ex.getMessage(), ex);
            throw invalidCode();
        }
    }

    private String extractEmail(String accessToken, Map<String, Object> githubUser) {
        Object email = githubUser.get("email");
        if (email instanceof String value && !value.isBlank()) {
            return value.trim().toLowerCase();
        }

        return fetchPrimaryVerifiedEmail(accessToken);
    }

    @SuppressWarnings("unchecked")
    private String fetchPrimaryVerifiedEmail(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri(GITHUB_EMAILS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.USER_AGENT, "ms-security")
                    .header(HttpHeaders.ACCEPT, GITHUB_ACCEPT)
                    .retrieve()
                    .body(List.class);

            if (emails == null) {
                throw noVerifiedEmail();
            }

            return emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                    .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                    .map(email -> String.valueOf(email.get("email")))
                    .findFirst()
                    .orElseGet(() -> emails.stream()
                            .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                            .map(email -> String.valueOf(email.get("email")))
                            .findFirst()
                            .orElseThrow(this::noVerifiedEmail));
        } catch (RestClientException ex) {
            log.error("Error al consultar los correos de GitHub: {}", ex.getMessage(), ex);
            throw noVerifiedEmail();
        }
    }

    private String extractName(Map<String, Object> githubUser, String email) {
        Object name = githubUser.get("name");
        if (name instanceof String value && !value.isBlank()) {
            return value.trim();
        }

        Object login = githubUser.get("login");
        if (login instanceof String value && !value.isBlank()) {
            return value.trim();
        }

        return email;
    }

    private ApplicationException invalidCode() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "Código de GitHub inválido o expirado"
        );
    }

    private ApplicationException noVerifiedEmail() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "La cuenta de GitHub no tiene un correo verificado"
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
