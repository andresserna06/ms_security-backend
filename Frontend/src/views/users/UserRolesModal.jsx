import React, { useCallback, useEffect, useState } from 'react'
import PropTypes from 'prop-types'
import {
  CAlert,
  CBadge,
  CButton,
  CCol,
  CFormSelect,
  CListGroup,
  CListGroupItem,
  CModal,
  CModalBody,
  CModalFooter,
  CModalHeader,
  CModalTitle,
  CRow,
  CSpinner,
} from '@coreui/react'
import CIcon from '@coreui/icons-react'
import { cilPlus, cilTrash } from '@coreui/icons'
import { userRoleService } from '../../services/userRoleService'

const UserRolesModal = ({ visible, user, onClose, onRolesUpdated }) => {
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [allRoles, setAllRoles] = useState([])
  const [assignedRoles, setAssignedRoles] = useState([])
  const [selectedRoleId, setSelectedRoleId] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const loadRolesData = useCallback(async () => {
    if (!user) {
      return
    }
    setLoading(true)
    setErrorMessage('')
    try {
      const [rolesCatalog, userRolesList] = await Promise.all([
        userRoleService.getAllRoles(),
        userRoleService.getUserRoles(user.id),
      ])
      setAllRoles(rolesCatalog || [])
      setAssignedRoles(userRolesList || [])
      setSelectedRoleId('')
    } catch (error) {
      setErrorMessage(error.message || 'Error al cargar los roles del usuario')
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    if (visible && user) {
      setSuccessMessage('')
      loadRolesData()
    }
  }, [visible, user, loadRolesData])

  const assignedRoleIds = new Set(assignedRoles.map((item) => item.role?.id))
  const availableRoles = allRoles.filter((role) => !assignedRoleIds.has(role.id))

  const handleAssignRole = async () => {
    if (!selectedRoleId || !user) {
      return
    }
    setSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')
    try {
      await userRoleService.assignRole(user.id, Number(selectedRoleId))
      await loadRolesData()
      setSuccessMessage('Rol asignado correctamente al usuario.')
      onRolesUpdated()
    } catch (error) {
      setErrorMessage(error.message || 'No fue posible asignar el rol')
    } finally {
      setSubmitting(false)
    }
  }

  const handleRemoveRole = async (roleId) => {
    if (!user) {
      return
    }
    setSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')
    try {
      await userRoleService.removeRole(user.id, roleId)
      await loadRolesData()
      setSuccessMessage('Rol retirado del usuario (el rol permanece en el sistema).')
      onRolesUpdated()
    } catch (error) {
      setErrorMessage(error.message || 'No fue posible retirar el rol')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <CModal visible={visible} onClose={onClose} backdrop="static" alignment="center" size="lg">
      <CModalHeader>
        <CModalTitle>Gestionar Roles — {user?.name}</CModalTitle>
      </CModalHeader>
      <CModalBody>
        {errorMessage && (
          <CAlert color="danger" className="py-2 small">
            {errorMessage}
          </CAlert>
        )}
        {successMessage && (
          <CAlert color="success" className="py-2 small">
            {successMessage}
          </CAlert>
        )}

        {loading ? (
          <div className="text-center py-4">
            <CSpinner color="primary" size="sm" />
            <div className="mt-2 small text-body-secondary">Cargando roles...</div>
          </div>
        ) : (
          <>
            <div className="mb-4">
              <label htmlFor="select-role" className="form-label fw-semibold small">
                Asignar nuevo rol al usuario
              </label>
              <CRow className="g-2">
                <CCol sm={8}>
                  <CFormSelect
                    id="select-role"
                    value={selectedRoleId}
                    onChange={(event) => setSelectedRoleId(event.target.value)}
                    disabled={submitting || availableRoles.length === 0}
                  >
                    <option value="">
                      {availableRoles.length === 0
                        ? 'No hay más roles disponibles para asignar'
                        : 'Selecciona un rol...'}
                    </option>
                    {availableRoles.map((role) => (
                      <option key={role.id} value={role.id}>
                        {role.name} — {role.description}
                      </option>
                    ))}
                  </CFormSelect>
                </CCol>
                <CCol sm={4}>
                  <CButton
                    color="primary"
                    className="w-100"
                    disabled={!selectedRoleId || submitting}
                    onClick={handleAssignRole}
                  >
                    <CIcon icon={cilPlus} className="me-1" />
                    Asignar rol
                  </CButton>
                </CCol>
              </CRow>
            </div>

            <div>
              <div className="fw-semibold small mb-2">
                Roles actualmente asignados ({assignedRoles.length})
              </div>
              {assignedRoles.length === 0 ? (
                <div className="text-body-secondary small fst-italic py-3 text-center border rounded">
                  Este usuario no tiene roles asignados actualmente.
                </div>
              ) : (
                <CListGroup>
                  {assignedRoles.map((assignment) => (
                    <CListGroupItem
                      key={assignment.id}
                      className="d-flex justify-content-between align-items-center"
                    >
                      <div>
                        <CBadge color="primary" className="me-2">
                          {assignment.role?.name}
                        </CBadge>
                        <span className="small text-body-secondary">
                          {assignment.role?.description}
                        </span>
                      </div>
                      <CButton
                        color="danger"
                        variant="outline"
                        size="sm"
                        disabled={submitting}
                        onClick={() => handleRemoveRole(assignment.role?.id)}
                        title="Retirar rol del usuario"
                      >
                        <CIcon icon={cilTrash} className="me-1" />
                        Retirar
                      </CButton>
                    </CListGroupItem>
                  ))}
                </CListGroup>
              )}
            </div>
          </>
        )}
      </CModalBody>
      <CModalFooter>
        <CButton color="secondary" onClick={onClose}>
          Cerrar
        </CButton>
      </CModalFooter>
    </CModal>
  )
}

UserRolesModal.propTypes = {
  visible: PropTypes.bool.isRequired,
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
  }),
  onClose: PropTypes.func.isRequired,
  onRolesUpdated: PropTypes.func.isRequired,
}

export default UserRolesModal
