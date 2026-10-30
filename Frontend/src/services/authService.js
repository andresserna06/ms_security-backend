import { apiClient } from './apiClient'

export const authService = {
  /**
   * Paso 1: Enviar credenciales e iniciar el desafío 2FA
   * @param {string} email
   * @param {string} password
   * @returns {Promise<{message: string, email: string}>}
   */
  login: async (email, password) => {
    return apiClient.post('/auth/login', {
      email: email.trim(),
      password,
    })
  },

  /**
   * Paso 2: Verificar el código 2FA de 6 dígitos recibido por correo
   * @param {string} email
   * @param {string} code
   * @returns {Promise<{accessToken: string, tokenType: string, expiresAt: string}>}
   */
  verify2Fa: async (email, code) => {
    const response = await apiClient.post('/auth/verify-2fa', {
      email: email.trim(),
      code: code.trim(),
    })

    if (response?.accessToken) {
      authService.setSession(response, email)
    }

    return response
  },

  /**
   * Cierre de sesión en el backend y limpieza de almacenamiento local
   */
  logout: async () => {
    try {
      await apiClient.post('/auth/logout')
    } catch (error) {
      // Si el servidor falla o ya expiró, continuamos con la limpieza local
      console.warn('Error al invalidar sesión en el servidor:', error)
    } finally {
      authService.clearSession()
    }
  },

  /**
   * Guarda los datos de sesión en localStorage
   */
  setSession: (authData, email = '') => {
    localStorage.setItem('accessToken', authData.accessToken)
    localStorage.setItem('token', authData.accessToken)
    localStorage.setItem('tokenType', authData.tokenType || 'Bearer')
    localStorage.setItem('tokenExpiresAt', authData.expiresAt || '')
    if (email) {
      localStorage.setItem('userEmail', email)
    }
  },

  /**
   * Obtiene el token JWT actual
   */
  getToken: () => {
    return localStorage.getItem('accessToken') || localStorage.getItem('token')
  },

  /**
   * Obtiene el correo del usuario autenticado
   */
  getUserEmail: () => {
    return localStorage.getItem('userEmail') || ''
  },

  /**
   * Verifica si existe un token en almacenamiento
   */
  isAuthenticated: () => {
    return Boolean(authService.getToken())
  },

  /**
   * Limpia todo rastro de la sesión local
   */
  clearSession: () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('token')
    localStorage.removeItem('tokenType')
    localStorage.removeItem('tokenExpiresAt')
    localStorage.removeItem('userEmail')
  },
}

export default authService
