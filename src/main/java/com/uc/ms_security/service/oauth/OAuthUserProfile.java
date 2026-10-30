package com.uc.ms_security.service.oauth;

/**
 * Datos de identidad neutrales extraídos de un proveedor externo.
 * No depende de ningún SDK concreto (Google, GitHub, etc.).
 */
public record OAuthUserProfile(
        String email,
        String name
) {
}

