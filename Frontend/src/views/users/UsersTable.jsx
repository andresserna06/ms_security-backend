import React from 'react'
import PropTypes from 'prop-types'
import {
  CBadge,
  CSpinner,
  CTable,
  CTableBody,
  CTableDataCell,
  CTableHead,
  CTableHeaderCell,
  CTableRow,
} from '@coreui/react'

const UsersTable = ({ users, loading, selectedUserId, onSelectUser }) => {
  if (loading) {
    return (
      <div className="text-center py-5">
        <CSpinner color="primary" />
        <div className="mt-2 text-body-secondary">Cargando listado de usuarios...</div>
      </div>
    )
  }

  if (!users || users.length === 0) {
    return (
      <div className="text-center py-5 text-body-secondary">
        No se encontraron usuarios registrados con los filtros seleccionados.
      </div>
    )
  }

  return (
    <CTable hover responsive align="middle" className="mb-0">
      <CTableHead color="light">
        <CTableRow>
          <CTableHeaderCell scope="col">ID</CTableHeaderCell>
          <CTableHeaderCell scope="col">Usuario</CTableHeaderCell>
          <CTableHeaderCell scope="col">Correo electrónico</CTableHeaderCell>
          <CTableHeaderCell scope="col" className="text-center">
            Estado
          </CTableHeaderCell>
        </CTableRow>
      </CTableHead>
      <CTableBody>
        {users.map((user) => {
          const isSelected = selectedUserId === user.id
          const isActive = user.active !== false

          return (
            <CTableRow
              key={user.id}
              active={isSelected}
              style={{ cursor: 'pointer' }}
              onClick={() => onSelectUser(user)}
            >
              <CTableDataCell className="fw-semibold text-body-secondary">
                #{user.id}
              </CTableDataCell>
              <CTableDataCell className="fw-semibold">{user.name}</CTableDataCell>
              <CTableDataCell>{user.email}</CTableDataCell>
              <CTableDataCell className="text-center">
                <CBadge color={isActive ? 'success' : 'secondary'} shape="rounded-pill">
                  {isActive ? 'Activo' : 'Inactivo'}
                </CBadge>
              </CTableDataCell>
            </CTableRow>
          )
        })}
      </CTableBody>
    </CTable>
  )
}

UsersTable.propTypes = {
  users: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
      email: PropTypes.string.isRequired,
      active: PropTypes.bool,
    }),
  ).isRequired,
  loading: PropTypes.bool.isRequired,
  selectedUserId: PropTypes.number,
  onSelectUser: PropTypes.func.isRequired,
}

export default UsersTable
