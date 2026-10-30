package com.uc.ms_security.service.oauth;

import com.uc.ms_security.entity.AuthProvider;

/**
 * Abstracción (DIP) de un proveedor de autenticación externo.
 * Los servicios de negocio dependen de esta interfaz y nunca de una implementación concreta.
 * Para soportar un nuevo proveedor basta con crear otro @Component que la implemente (OCP).
 */
public interface OAuthProviderStrategy {

    /**
     * Proveedor que atiende esta estrategia.
     */
    AuthProvider getProviderName();

    /**
     * Valida el token de identidad emitido por el proveedor y extrae el perfil del usuario.
     *
     * @param idToken token de identidad recibido desde el cliente
     * @return perfil neutral del usuario autenticado
     * @throws com.uc.ms_security.exception.ApplicationException si el token no es válido
     */
    OAuthUserProfile verifyAndExtractUser(String idToken);
}

