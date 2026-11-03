package com.uc.ms_security.entity;

/**
 * Mecanismo con el que se registró y autentica un usuario.
 * LOCAL: correo y contraseña (con 2FA). GOOGLE: Google OAuth 2.0. AZURE / MICROSOFT: Microsoft Azure OAuth 2.0. GITHUB: GitHub OAuth.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    AZURE,
    MICROSOFT,
    GITHUB
}
