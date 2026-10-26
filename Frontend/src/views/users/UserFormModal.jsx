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
  CFormSelect,
  CModal,
  CModalBody,
  CModalFooter,
  CModalHeader,
  CModalTitle,
  CRow,
  CSpinner,
} from '@coreui/react'

const initialFormState = {
  name: '',
  email: '',
  password: '',
  active: true,
}

const UserFormModal = ({ visible, userToEdit, submitting, onClose, onSubmit }) => {
  const isEditMode = Boolean(userToEdit)
  const [formData, setFormData] = useState(initialFormState)
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')

  useEffect(() => {
    if (visible) {
      if (userToEdit) {
        setFormData({
          name: userToEdit.name || '',
          email: userToEdit.email || '',
          password: '',
          active: userToEdit.active !== false,
        })
      } else {
        setFormData(initialFormState)
      }
      setErrors({})
      setServerError('')
    }
  }, [visible, userToEdit])

  const validateForm = () => {
    const validationErrors = {}

    if (!formData.name || formData.name.trim().length < 2) {
      validationErrors.name = 'El nombre debe tener entre 2 y 100 caracteres'
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!formData.email || !emailRegex.test(formData.email.trim())) {
      validationErrors.email = 'Ingresa un correo electrónico válido'
    }

    if (!isEditMode) {
      if (!formData.password || formData.password.length < 8) {
        validationErrors.password = 'La contraseña es obligatoria y debe tener mínimo 8 caracteres'
      }
    } else if (formData.password && formData.password.length < 8) {
      validationErrors.password = 'Si deseas cambiar la contraseña, debe tener mínimo 8 caracteres'
    }

    setErrors(validationErrors)
    return Object.keys(validationErrors).length === 0
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'active' ? value === 'true' : value,
    }))
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }))
    }
    if (serverError) {
      setServerError('')
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!validateForm()) {
      return
    }

    const payload = {
      name: formData.name.trim(),
      email: formData.email.trim(),
    }

    if (!isEditMode) {
      payload.password = formData.password
    } else {
      payload.active = formData.active
      if (formData.password && formData.password.trim().length > 0) {
        payload.password = formData.password
      }
    }

    try {
      await onSubmit(payload)
    } catch (error) {
      if (error.fieldErrors) {
        setErrors((prev) => ({ ...prev, ...error.fieldErrors }))
      }
      setServerError(error.message || 'No fue posible guardar el usuario')
    }
  }

  return (
    <CModal visible={visible} onClose={onClose} backdrop="static" alignment="center">
      <CForm onSubmit={handleSubmit} noValidate>
        <CModalHeader>
          <CModalTitle>{isEditMode ? 'Editar usuario' : 'Registrar nuevo usuario'}</CModalTitle>
        </CModalHeader>
        <CModalBody>
          {serverError && (
            <CAlert color="danger" className="py-2 small">
              {serverError}
            </CAlert>
          )}

          <CRow className="g-3">
            <CCol xs={12}>
              <CFormLabel htmlFor="user-name">
                Nombre completo <span className="text-danger">*</span>
              </CFormLabel>
              <CFormInput
                id="user-name"
                name="name"
                placeholder="Ej. Carlos Administrador"
                value={formData.name}
                onChange={handleChange}
                invalid={Boolean(errors.name)}
                disabled={submitting}
              />
              {errors.name && <CFormFeedback invalid>{errors.name}</CFormFeedback>}
            </CCol>

            <CCol xs={12}>
              <CFormLabel htmlFor="user-email">
                Correo electrónico <span className="text-danger">*</span>
              </CFormLabel>
              <CFormInput
                id="user-email"
                type="email"
                name="email"
                placeholder="Ej. usuario@correo.com"
                value={formData.email}
                onChange={handleChange}
                invalid={Boolean(errors.email)}
                disabled={submitting}
              />
              {errors.email && <CFormFeedback invalid>{errors.email}</CFormFeedback>}
            </CCol>

            <CCol xs={12}>
              <CFormLabel htmlFor="user-password">
                Contraseña {!isEditMode && <span className="text-danger">*</span>}
              </CFormLabel>
              <CFormInput
                id="user-password"
                type="password"
                name="password"
                placeholder={
                  isEditMode
                    ? 'Dejar en blanco para mantener la contraseña actual'
                    : 'Mínimo 8 caracteres'
                }
                value={formData.password}
                onChange={handleChange}
                invalid={Boolean(errors.password)}
                disabled={submitting}
              />
              {errors.password && <CFormFeedback invalid>{errors.password}</CFormFeedback>}
            </CCol>

            {isEditMode && (
              <CCol xs={12}>
                <CFormLabel htmlFor="user-active">Estado del usuario</CFormLabel>
                <CFormSelect
                  id="user-active"
                  name="active"
                  value={String(formData.active)}
                  onChange={handleChange}
                  disabled={submitting}
                >
                  <option value="true">Activo</option>
                  <option value="false">Inactivo</option>
                </CFormSelect>
              </CCol>
            )}
          </CRow>
        </CModalBody>
        <CModalFooter>
          <CButton color="secondary" variant="outline" onClick={onClose} disabled={submitting}>
            Cancelar
          </CButton>
          <CButton color="primary" type="submit" disabled={submitting}>
            {submitting ? (
              <>
                <CSpinner size="sm" className="me-2" />
                Guardando...
              </>
            ) : isEditMode ? (
              'Guardar cambios'
            ) : (
              'Registrar usuario'
            )}
          </CButton>
        </CModalFooter>
      </CForm>
    </CModal>
  )
}

UserFormModal.propTypes = {
  visible: PropTypes.bool.isRequired,
  userToEdit: PropTypes.shape({
    id: PropTypes.number,
    name: PropTypes.string,
    email: PropTypes.string,
    active: PropTypes.bool,
  }),
  submitting: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
}

export default UserFormModal
