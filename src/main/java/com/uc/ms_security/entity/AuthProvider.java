package com.uc.ms_security.entity;

/**
 * Mecanismo con el que se registró y autentica un usuario.
 * LOCAL: correo y contraseña (con 2FA).
 * GOOGLE / GITHUB: OAuth 2.0 (sin contraseña ni 2FA).
 * AZURE / MICROSOFT: Microsoft Azure OAuth 2.0.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    AZURE,
    MICROSOFT,
    GITHUB
}
