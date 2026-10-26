import React, { useEffect, useState } from 'react'
import PropTypes from 'prop-types'
import {
  CAlert,
  CButton,
  CCol,
  CForm,
  CFormFeedback,
  CFormInput,
  CFormLabel,
  CModal,
  CModalBody,
  CModalFooter,
  CModalHeader,
  CModalTitle,
  CRow,
  CSpinner,
} from '@coreui/react'
import { profileService } from '../../services/profileService'

const UserProfileModal = ({ visible, user, onClose, onSaved }) => {
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [hasExistingProfile, setHasExistingProfile] = useState(false)
  const [formData, setFormData] = useState({
    phone: '',
    birthDate: '',
  })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')

  useEffect(() => {
    const fetchProfile = async () => {
      if (!visible || !user) {
        return
      }
      setLoading(true)
      setErrors({})
      setServerError('')

      try {
        const profile = await profileService.getByUserId(user.id)
        if (profile && profile.id) {
          setHasExistingProfile(true)
          setFormData({
            phone: profile.phone || '',
            birthDate: profile.birthDate || '',
          })
        } else {
          setHasExistingProfile(false)
          setFormData({ phone: '', birthDate: '' })
        }
      } catch (error) {
        if (error.status === 404) {
          setHasExistingProfile(false)
          setFormData({ phone: '', birthDate: '' })
        } else {
          setServerError(error.message || 'Error al consultar el perfil del usuario')
        }
      } finally {
        setLoading(false)
      }
    }

    fetchProfile()
  }, [visible, user])

  const validate = () => {
    const nextErrors = {}
    if (!formData.phone || formData.phone.trim().length < 7 || formData.phone.trim().length > 20) {
      nextErrors.phone = 'El teléfono debe tener entre 7 y 20 caracteres'
    }
    if (!formData.birthDate) {
      nextErrors.birthDate = 'La fecha de nacimiento es obligatoria'
    }
    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }))
    }
    if (serverError) {
      setServerError('')
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!validate() || !user) {
      return
    }

    setSubmitting(true)
    setServerError('')
    try {
      const payload = {
        phone: formData.phone.trim(),
        birthDate: formData.birthDate,
      }
      if (hasExistingProfile) {
        await profileService.update(user.id, payload)
      } else {
        await profileService.create(user.id, payload)
      }
      onSaved(
        hasExistingProfile
          ? `Perfil de ${user.name} actualizado correctamente`
          : `Perfil de ${user.name} creado correctamente`,
      )
      onClose()
    } catch (error) {
      if (error.fieldErrors) {
        setErrors((prev) => ({ ...prev, ...error.fieldErrors }))
      }
      setServerError(error.message || 'No se pudo guardar el perfil')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDeleteProfile = async () => {
    if (!user || !hasExistingProfile) {
      return
    }
    setSubmitting(true)
    setServerError('')
    try {
      await profileService.remove(user.id)
      onSaved(`Perfil de ${user.name} eliminado correctamente`)
      onClose()
    } catch (error) {
      setServerError(error.message || 'No se pudo eliminar el perfil')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <CModal visible={visible} onClose={onClose} backdrop="static" alignment="center">
      <CForm onSubmit={handleSubmit} noValidate>
        <CModalHeader>
          <CModalTitle>Gestionar Perfil — {user?.name}</CModalTitle>
        </CModalHeader>
        <CModalBody>
          {serverError && (
            <CAlert color="danger" className="py-2 small">
              {serverError}
            </CAlert>
          )}

          {loading ? (
            <div className="text-center py-4">
              <CSpinner color="primary" size="sm" />
              <div className="mt-2 small text-body-secondary">Cargando datos del perfil...</div>
            </div>
          ) : (
            <CRow className="g-3">
              <CCol xs={12}>
                <CFormLabel htmlFor="profile-phone">
                  Teléfono de contacto <span className="text-danger">*</span>
                </CFormLabel>
                <CFormInput
                  id="profile-phone"
                  name="phone"
                  placeholder="Ej. 3001234567"
                  value={formData.phone}
                  onChange={handleChange}
                  invalid={Boolean(errors.phone)}
                  disabled={submitting}
                />
                {errors.phone && <CFormFeedback invalid>{errors.phone}</CFormFeedback>}
              </CCol>

              <CCol xs={12}>
                <CFormLabel htmlFor="profile-birthDate">
                  Fecha de nacimiento <span className="text-danger">*</span>
                </CFormLabel>
                <CFormInput
                  id="profile-birthDate"
                  type="date"
                  name="birthDate"
                  value={formData.birthDate}
                  onChange={handleChange}
                  invalid={Boolean(errors.birthDate)}
                  disabled={submitting}
                />
                {errors.birthDate && <CFormFeedback invalid>{errors.birthDate}</CFormFeedback>}
              </CCol>
            </CRow>
          )}
        </CModalBody>
        <CModalFooter className="d-flex justify-content-between">
          <div>
            {hasExistingProfile && (
              <CButton
                color="danger"
                variant="outline"
                onClick={handleDeleteProfile}
                disabled={submitting || loading}
              >
                Eliminar perfil
              </CButton>
            )}
          </div>
          <div className="d-flex gap-2">
            <CButton
              color="secondary"
              variant="outline"
              onClick={onClose}
              disabled={submitting}
            >
              Cancelar
            </CButton>
            <CButton color="primary" type="submit" disabled={submitting || loading}>
              {submitting ? (
                <>
                  <CSpinner size="sm" className="me-2" />
                  Guardando...
                </>
              ) : hasExistingProfile ? (
                'Actualizar perfil'
              ) : (
                'Crear perfil'
              )}
            </CButton>
          </div>
        </CModalFooter>
      </CForm>
    </CModal>
  )
}

UserProfileModal.propTypes = {
  visible: PropTypes.bool.isRequired,
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    email: PropTypes.string,
  }),
  onClose: PropTypes.func.isRequired,
  onSaved: PropTypes.func.isRequired,
}

export default UserProfileModal
