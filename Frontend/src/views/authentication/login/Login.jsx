import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  CAlert,
  CButton,
  CCard,
  CCardBody,
  CCol,
  CContainer,
  CForm,
  CFormCheck,
  CFormInput,
  CFormLabel,
  CInputGroup,
  CInputGroupText,
  CRow,
  CSpinner,
  CTooltip,
} from '@coreui/react'
import CIcon from '@coreui/icons-react'
import { apple } from 'src/assets/brand/apple'
import { google } from 'src/assets/brand/google'
import { eye } from 'src/assets/icons/eye'
import { authService } from 'src/services/authService'
import jpaSports from '../../../assets/images/JPASports.jpg'

const Login = () => {
  const navigate = useNavigate()

  // Estados del flujo de autenticación
  const [step, setStep] = useState('CREDENTIALS') // 'CREDENTIALS' | '2FA'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [code2Fa, setCode2Fa] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(false)

  // Estados de retroalimentación visual
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [infoMessage, setInfoMessage] = useState('')

  // Manejador del Paso 1: Enviar credenciales
  const handleLoginSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setInfoMessage('')

    if (!email.trim() || !password) {
      setError('Por favor ingresa tu correo y contraseña.')
      return
    }

    try {
      setLoading(true)
      const response = await authService.login(email, password)
      setInfoMessage(
        response?.message || 'Código de verificación de 6 dígitos enviado a tu correo.',
      )
      setStep('2FA')
    } catch (err) {
      setError(err?.message || 'Credenciales incorrectas o usuario inhabilitado.')
    } finally {
      setLoading(false)
    }
  }

  // Manejador del Paso 2: Verificar código 2FA
  const handleVerify2FaSubmit = async (e) => {
    e.preventDefault()
    setError('')

    const cleanCode = code2Fa.trim()
    if (!/^\d{6}$/.test(cleanCode)) {
      setError('El código 2FA debe tener exactamente 6 dígitos numéricos.')
      return
    }

    try {
      setLoading(true)
      await authService.verify2Fa(email, cleanCode)
      // Redirigir al dashboard al completar la autenticación
      navigate('/dashboard')
    } catch (err) {
      setError(err?.message || 'Código incorrecto o expirado.')
    } finally {
      setLoading(false)
    }
  }

  // Reenviar código 2FA
  const handleResendCode = async () => {
    setError('')
    setInfoMessage('')
    try {
      setLoading(true)
      const response = await authService.login(email, password)
      setInfoMessage(
        response?.message || 'Se ha reenviado un nuevo código a tu correo electrónico.',
      )
    } catch (err) {
      setError(err?.message || 'Error al reenviar el código. Intenta nuevamente.')
    } finally {
      setLoading(false)
    }
  }

  // Volver al formulario de credenciales
  const handleBackToCredentials = () => {
    setStep('CREDENTIALS')
    setCode2Fa('')
    setError('')
    setInfoMessage('')
  }

  return (
    <div className="bg-body-tertiary min-vh-100 d-flex flex-row align-items-center py-4">
      <CContainer>
        <CRow className="justify-content-center">
          <CCol md={8} lg={6} xl={5}>
            <div className="d-flex flex-column gap-4">
              <div className="text-center">
                <img src={jpaSports} alt="JPA Sports" height="96" />
              </div>

              <CCard className="p-4 shadow-sm border-0">
                <CCardBody className="d-flex flex-column gap-3">
                  {error && (
                    <CAlert color="danger" dismissible onClose={() => setError('')}>
                      {error}
                    </CAlert>
                  )}

                  {infoMessage && (
                    <CAlert color="info" dismissible onClose={() => setInfoMessage('')}>
                      {infoMessage}
                    </CAlert>
                  )}

                  {step === 'CREDENTIALS' ? (
                    <>
                      <h2 className="h5 text-center mb-0 fw-bold">Iniciar sesión en tu cuenta</h2>
                      <p className="text-body-secondary text-center small mb-2">
                        Ingresa tus credenciales para recibir tu código de seguridad
                      </p>

                      <CForm className="row gy-3" onSubmit={handleLoginSubmit}>
                        <CCol xs={12}>
                          <CFormLabel htmlFor="email">Correo electrónico</CFormLabel>
                          <CFormInput
                            id="email"
                            type="email"
                            placeholder="tu@correo.com"
                            autoComplete="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            disabled={loading}
                          />
                        </CCol>

                        <CCol xs={12}>
                          <div className="d-flex justify-content-between">
                            <CFormLabel htmlFor="password">Contraseña</CFormLabel>
                            <Link to="/authentication/reset-password" style={{ color: '#70B038' }}>
                              ¿Olvidaste tu contraseña?
                            </Link>
                          </div>
                          <CInputGroup>
                            <CFormInput
                              id="password"
                              type={showPassword ? 'text' : 'password'}
                              placeholder="Tu contraseña"
                              autoComplete="current-password"
                              value={password}
                              onChange={(e) => setPassword(e.target.value)}
                              required
                              disabled={loading}
                            />
                            <CInputGroupText>
                              <CTooltip content={showPassword ? 'Ocultar contraseña' : 'Ver contraseña'}>
                                <CButton
                                  type="button"
                                  color="link"
                                  className="p-0 link-secondary"
                                  aria-label="Toggle password"
                                  onClick={() => setShowPassword(!showPassword)}
                                >
                                  <CIcon icon={eye} size="sm" />
                                </CButton>
                              </CTooltip>
                            </CInputGroupText>
                          </CInputGroup>
                        </CCol>

                        <CCol xs={12}>
                          <CFormCheck
                            id="rememberMe"
                            label="Recordarme en este dispositivo"
                            checked={rememberMe}
                            onChange={(e) => setRememberMe(e.target.checked)}
                            disabled={loading}
                          />
                        </CCol>

                        <CCol xs={12}>
                          <CButton
                            type="submit"
                            className="w-100 text-white fw-semibold py-2"
                            style={{ backgroundColor: '#70B038', borderColor: '#70B038' }}
                            disabled={loading}
                          >
                            {loading ? (
                              <>
                                <CSpinner size="sm" className="me-2" />
                                Validando credenciales...
                              </>
                            ) : (
                              'Continuar'
                            )}
                          </CButton>
                        </CCol>
                      </CForm>

                      <div className="position-relative my-2">
                        <hr />
                        <div className="position-absolute top-50 start-50 translate-middle bg-body px-2 text-body-tertiary text-uppercase small">
                          o
                        </div>
                      </div>

                      <CRow className="g-2">
                        <CCol xs={12} sm={6}>
                          <CButton type="button" variant="outline" className="w-100" disabled={loading}>
                            <CIcon icon={google} className="me-1" />
                            Google
                          </CButton>
                        </CCol>
                        <CCol xs={12} sm={6}>
                          <CButton type="button" variant="outline" className="w-100" disabled={loading}>
                            <CIcon icon={apple} className="me-1" />
                            Apple
                          </CButton>
                        </CCol>
                      </CRow>
                    </>
                  ) : (
                    /* PASO 2: Verificación del Código 2FA */
                    <>
                      <div className="text-center">
                        <div
                          className="d-inline-flex align-items-center justify-content-center rounded-circle mb-2"
                          style={{
                            width: '48px',
                            height: '48px',
                            backgroundColor: 'rgba(112, 176, 56, 0.15)',
                            color: '#70B038',
                          }}
                        >
                          <svg
                            width="24"
                            height="24"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          >
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                          </svg>
                        </div>
                        <h2 className="h5 fw-bold mb-1">Verificación de Dos Factores (2FA)</h2>
                        <p className="text-body-secondary small mb-3">
                          Ingresa el código numérico de 6 dígitos enviado a <br />
                          <strong>{email}</strong>
                        </p>
                      </div>

                      <CForm className="row gy-3" onSubmit={handleVerify2FaSubmit}>
                        <CCol xs={12}>
                          <CFormLabel htmlFor="code2Fa" className="text-center d-block fw-semibold">
                            Código de Verificación
                          </CFormLabel>
                          <CFormInput
                            id="code2Fa"
                            type="text"
                            inputMode="numeric"
                            pattern="[0-9]*"
                            maxLength={6}
                            placeholder="••••••"
                            autoComplete="one-time-code"
                            value={code2Fa}
                            onChange={(e) => {
                              const val = e.target.value.replace(/\D/g, '')
                              setCode2Fa(val)
                            }}
                            className="text-center fs-3 tracking-wider py-2"
                            style={{ letterSpacing: '0.4em', fontWeight: 'bold' }}
                            autoFocus
                            required
                            disabled={loading}
                          />
                          <div className="text-center text-body-secondary small mt-2">
                            El código es válido por 5 minutos y de un solo uso.
                          </div>
                        </CCol>

                        <CCol xs={12}>
                          <CButton
                            type="submit"
                            className="w-100 text-white fw-semibold py-2"
                            style={{ backgroundColor: '#70B038', borderColor: '#70B038' }}
                            disabled={loading || code2Fa.length !== 6}
                          >
                            {loading ? (
                              <>
                                <CSpinner size="sm" className="me-2" />
                                Verificando código...
                              </>
                            ) : (
                              'Verificar y Acceder'
                            )}
                          </CButton>
                        </CCol>

                        <CCol xs={12} className="d-flex justify-content-between align-items-center pt-2">
                          <CButton
                            type="button"
                            color="link"
                            className="p-0 text-decoration-none small text-body-secondary"
                            onClick={handleBackToCredentials}
                            disabled={loading}
                          >
                            ← Cambiar credenciales
                          </CButton>
                          <CButton
                            type="button"
                            color="link"
                            className="p-0 text-decoration-none small fw-semibold"
                            style={{ color: '#70B038' }}
                            onClick={handleResendCode}
                            disabled={loading}
                          >
                            Reenviar código
                          </CButton>
                        </CCol>
                      </CForm>
                    </>
                  )}
                </CCardBody>
              </CCard>

              <div className="text-center text-body-secondary">
                ¿No tienes una cuenta?{' '}
                <Link to="/authentication/register" style={{ color: '#70B038' }}>
                  Regístrate
                </Link>
              </div>
            </div>
          </CCol>
        </CRow>
      </CContainer>
    </div>
  )
}

export default Login
