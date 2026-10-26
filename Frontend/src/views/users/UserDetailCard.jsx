import React from 'react'
import PropTypes from 'prop-types'
import {
  CAlert,
  CBadge,
  CButton,
  CCard,
  CCardBody,
  CCardHeader,
  CCol,
  CListGroup,
  CListGroupItem,
  CRow,
  CSpinner,
} from '@coreui/react'
import CIcon from '@coreui/icons-react'
import {
  cilCheckCircle,
  cilContact,
  cilPencil,
  cilShieldAlt,
  cilTrash,
  cilUser,
  cilXCircle,
} from '@coreui/icons'

const UserDetailCard = ({
  userDetail,
  userRoles,
  loadingDetail,
  onEditUser,
  onManageProfile,
  onManageRoles,
  onToggleStatus,
  onDeleteUser,
}) => {
  if (!userDetail && loadingDetail) {
    return (
      <CCard className="h-100 shadow-sm">
        <CCardHeader className="fw-semibold">Detalle del usuario</CCardHeader>
        <CCardBody className="text-center py-5">
          <CSpinner color="primary" size="sm" />
          <div className="mt-2 text-body-secondary small">Cargando detalle del usuario...</div>
        </CCardBody>
      </CCard>
    )
  }

  if (!userDetail) {
    return (
      <CCard className="h-100 shadow-sm">
        <CCardHeader className="fw-semibold">Detalle del usuario</CCardHeader>
        <CCardBody className="text-center py-5 text-body-secondary">
          <CIcon icon={cilUser} size="xl" className="mb-2 opacity-50" />
          <p className="mb-0">
            Selecciona un usuario de la tabla para consultar su información detallada y gestionar
            su perfil, roles y estado.
          </p>
        </CCardBody>
      </CCard>
    )
  }

  const isActive = userDetail.active !== false
  const profile = userDetail.profile
  const roles = userRoles || []

  return (
    <CCard className="h-100 shadow-sm">
      <CCardHeader className="d-flex justify-content-between align-items-center">
        <span className="fw-semibold">Detalle del usuario</span>
        <div className="d-flex align-items-center gap-2">
          {loadingDetail && <CSpinner color="primary" size="sm" />}
          <CBadge color="info" shape="rounded-pill">
            ID: #{userDetail.id}
          </CBadge>
        </div>
      </CCardHeader>
      <CCardBody>
        <div className="d-flex justify-content-between align-items-start mb-3">
          <div>
            <h5 className="mb-1 fw-bold">{userDetail.name}</h5>
            <div className="text-body-secondary small">{userDetail.email}</div>
          </div>
          <CBadge color={isActive ? 'success' : 'secondary'} shape="rounded-pill">
            {isActive ? 'Activo' : 'Inactivo'}
          </CBadge>
        </div>

        <CListGroup flush className="border-top border-bottom mb-3">
          <CListGroupItem className="d-flex justify-content-between px-0 py-2">
            <span className="text-body-secondary small">Nombre completo</span>
            <span className="fw-semibold small">{userDetail.name}</span>
          </CListGroupItem>
          <CListGroupItem className="d-flex justify-content-between px-0 py-2">
            <span className="text-body-secondary small">Correo electrónico</span>
            <span className="fw-semibold small">{userDetail.email}</span>
          </CListGroupItem>
          <CListGroupItem className="d-flex justify-content-between px-0 py-2">
            <span className="text-body-secondary small">Teléfono (Perfil)</span>
            <span className="fw-semibold small">
              {profile?.phone || (
                <span className="text-body-secondary fst-italic">Sin registrar</span>
              )}
            </span>
          </CListGroupItem>
          <CListGroupItem className="d-flex justify-content-between px-0 py-2">
            <span className="text-body-secondary small">Fecha de nacimiento</span>
            <span className="fw-semibold small">
              {profile?.birthDate || (
                <span className="text-body-secondary fst-italic">Sin registrar</span>
              )}
            </span>
          </CListGroupItem>
        </CListGroup>

        <div className="mb-3">
          <div className="d-flex justify-content-between align-items-center mb-2">
            <span className="fw-semibold small text-uppercase text-body-secondary">
              Roles asociados ({roles.length})
            </span>
          </div>
          {roles.length === 0 ? (
            <div className="text-body-secondary small fst-italic">
              Este usuario aún no tiene roles asignados.
            </div>
          ) : (
            <div className="d-flex flex-wrap gap-2">
              {roles.map((userRole) => (
                <CBadge
                  key={userRole.id || userRole.role?.id}
                  color="primary"
                  shape="rounded-pill"
                  className="px-3 py-2"
                >
                  {userRole.role?.name}
                </CBadge>
              ))}
            </div>
          )}
        </div>

        <CRow className="g-2 mb-3">
          <CCol xs={12}>
            <CButton
              color="primary"
              size="sm"
              className="w-100 fw-semibold"
              onClick={() => onEditUser(userDetail)}
            >
              <CIcon icon={cilPencil} className="me-2" />
              Editar usuario
            </CButton>
          </CCol>
          <CCol xs={6}>
            <CButton
              color="info"
              size="sm"
              className="w-100 text-white fw-semibold"
              onClick={() => onManageProfile(userDetail)}
            >
              <CIcon icon={cilContact} className="me-1" />
              Gestionar Perfil
            </CButton>
          </CCol>
          <CCol xs={6}>
            <CButton
              color="secondary"
              size="sm"
              className="w-100 text-white fw-semibold"
              onClick={() => onManageRoles(userDetail)}
            >
              <CIcon icon={cilShieldAlt} className="me-1" />
              Gestionar Roles
            </CButton>
          </CCol>
          <CCol xs={6}>
            <CButton
              color={isActive ? 'warning' : 'success'}
              size="sm"
              className="w-100 text-white fw-semibold"
              onClick={() => onToggleStatus(userDetail)}
            >
              <CIcon icon={isActive ? cilXCircle : cilCheckCircle} className="me-1" />
              {isActive ? 'Desactivar' : 'Activar'}
            </CButton>
          </CCol>
          <CCol xs={6}>
            <CButton
              color="danger"
              size="sm"
              className="w-100 text-white fw-semibold"
              onClick={() => onDeleteUser(userDetail)}
            >
              <CIcon icon={cilTrash} className="me-1" />
              Eliminar
            </CButton>
          </CCol>
        </CRow>

        <CAlert color="info" className="mb-0 py-2 px-3 small">
          La eliminación o desactivación de un usuario no elimina los roles ni permisos existentes
          en el sistema.
        </CAlert>
      </CCardBody>
    </CCard>
  )
}

UserDetailCard.propTypes = {
  userDetail: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    email: PropTypes.string.isRequired,
    active: PropTypes.bool,
    profile: PropTypes.shape({
      id: PropTypes.number,
      phone: PropTypes.string,
      birthDate: PropTypes.string,
    }),
  }),
  userRoles: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number,
      userId: PropTypes.number,
      role: PropTypes.shape({
        id: PropTypes.number,
        name: PropTypes.string,
        description: PropTypes.string,
      }),
    }),
  ),
  loadingDetail: PropTypes.bool.isRequired,
  onEditUser: PropTypes.func.isRequired,
  onManageProfile: PropTypes.func.isRequired,
  onManageRoles: PropTypes.func.isRequired,
  onToggleStatus: PropTypes.func.isRequired,
  onDeleteUser: PropTypes.func.isRequired,
}

export default UserDetailCard
