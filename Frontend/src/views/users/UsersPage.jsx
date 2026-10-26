import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  CAlert,
  CButton,
  CCard,
  CCardBody,
  CCardHeader,
  CCol,
  CFormInput,
  CFormSelect,
  CInputGroup,
  CInputGroupText,
  CModal,
  CModalBody,
  CModalFooter,
  CModalHeader,
  CModalTitle,
  CRow,
  CSpinner,
} from '@coreui/react'
import CIcon from '@coreui/icons-react'
import { cilPlus, cilReload, cilSearch } from '@coreui/icons'
import { userService } from '../../services/userService'
import UsersTable from './UsersTable'
import UserDetailCard from './UserDetailCard'
import UserFormModal from './UserFormModal'
import UserProfileModal from './UserProfileModal'
import UserRolesModal from './UserRolesModal'

const UsersPage = () => {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  const [selectedUserId, setSelectedUserId] = useState(null)
  const [selectedUserDetail, setSelectedUserDetail] = useState(null)
  const [selectedUserRoles, setSelectedUserRoles] = useState([])
  const [loadingDetail, setLoadingDetail] = useState(false)

  const [feedback, setFeedback] = useState({ type: '', message: '' })

  // Estado de modales
  const [formModalVisible, setFormModalVisible] = useState(false)
  const [userToEdit, setUserToEdit] = useState(null)
  const [submittingForm, setSubmittingForm] = useState(false)

  const [profileModalUser, setProfileModalUser] = useState(null)
  const [rolesModalUser, setRolesModalUser] = useState(null)
  const [userToDelete, setUserToDelete] = useState(null)
  const [deleting, setDeleting] = useState(false)

  const showNotification = (type, message) => {
    setFeedback({ type, message })
  }

  const fetchUsers = useCallback(async (showSpinner = true) => {
    if (showSpinner) {
      setLoading(true)
    }
    try {
      const data = await userService.getAll()
      const userList = Array.isArray(data) ? data : []
      setUsers(userList)
      setSelectedUserId((currentId) => {
        if (currentId && userList.some((u) => u.id === currentId)) {
          return currentId
        }
        return userList.length > 0 ? userList[0].id : null
      })
    } catch (error) {
      showNotification(
        'danger',
        error.message || 'No fue posible conectar con el servidor para obtener los usuarios.',
      )
    } finally {
      if (showSpinner) {
        setLoading(false)
      }
    }
  }, [])

  const fetchUserDetail = useCallback(async (userId) => {
    if (!userId) {
      setSelectedUserDetail(null)
      setSelectedUserRoles([])
      return
    }

    setLoadingDetail(true)
    try {
      const [detailWithProfile, detailWithRoles] = await Promise.all([
        userService.getDetailWithProfile(userId),
        userService.getDetailWithRoles(userId),
      ])
      setSelectedUserDetail(detailWithProfile)
      setSelectedUserRoles(detailWithRoles?.roles || [])
    } catch (error) {
      showNotification('danger', error.message || 'Error al cargar el detalle del usuario')
    } finally {
      setLoadingDetail(false)
    }
  }, [])

  useEffect(() => {
    fetchUsers(true)
  }, [fetchUsers])

  useEffect(() => {
    if (selectedUserId) {
      fetchUserDetail(selectedUserId)
    }
  }, [selectedUserId, fetchUserDetail])

  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      const matchesSearch =
        user.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.email?.toLowerCase().includes(searchTerm.toLowerCase())

      const isActive = user.active !== false
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && isActive) ||
        (statusFilter === 'INACTIVE' && !isActive)

      return matchesSearch && matchesStatus
    })
  }, [users, searchTerm, statusFilter])

  const handleOpenCreateModal = () => {
    setUserToEdit(null)
    setFormModalVisible(true)
  }

  const handleOpenEditModal = (user) => {
    setUserToEdit(user)
    setFormModalVisible(true)
  }

  const handleSubmitUserForm = async (payload) => {
    setSubmittingForm(true)
    try {
      if (userToEdit) {
        const updated = await userService.update(userToEdit.id, payload)
        setUsers((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
        showNotification('success', `Usuario "${updated.name}" actualizado correctamente.`)
        setFormModalVisible(false)
        await fetchUserDetail(updated.id)
      } else {
        const created = await userService.create(payload)
        setUsers((prev) => [...prev, created])
        setSelectedUserId(created.id)
        showNotification('success', `Usuario "${created.name}" registrado correctamente.`)
        setFormModalVisible(false)
      }
    } finally {
      setSubmittingForm(false)
    }
  }

  const handleToggleStatus = async (user) => {
    const isActive = user.active !== false
    try {
      const updatedUser = isActive
        ? await userService.deactivate(user.id)
        : await userService.activate(user.id)

      setUsers((prev) => prev.map((item) => (item.id === updatedUser.id ? updatedUser : item)))
      setSelectedUserDetail((prev) =>
        prev && prev.id === updatedUser.id ? { ...prev, active: updatedUser.active } : prev,
      )

      if (isActive) {
        showNotification(
          'warning',
          `El usuario "${user.name}" ha sido desactivado. Sus roles y permisos del sistema se mantienen intactos.`,
        )
      } else {
        showNotification('success', `El usuario "${user.name}" ha sido activado nuevamente.`)
      }
    } catch (error) {
      showNotification('danger', error.message || 'No se pudo cambiar el estado del usuario.')
    }
  }

  const handleConfirmDelete = async () => {
    if (!userToDelete) {
      return
    }
    setDeleting(true)
    try {
      await userService.remove(userToDelete.id)
      showNotification(
        'success',
        `Usuario "${userToDelete.name}" eliminado correctamente. Los roles y permisos del sistema permanecen intactos.`,
      )
      const deletedId = userToDelete.id
      setUserToDelete(null)

      const remainingUsers = users.filter((item) => item.id !== deletedId)
      setUsers(remainingUsers)

      if (selectedUserId === deletedId) {
        const nextId = remainingUsers.length > 0 ? remainingUsers[0].id : null
        setSelectedUserId(nextId)
        if (!nextId) {
          setSelectedUserDetail(null)
          setSelectedUserRoles([])
        }
      }
    } catch (error) {
      showNotification('danger', error.message || 'No fue posible eliminar el usuario.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="pb-4">
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <div>
          <h3 className="mb-1 fw-bold">Gestión de Usuarios</h3>
          <div className="text-body-secondary small">
            Administra los usuarios registrados en la plataforma, su estado, perfil y roles de
            acceso.
          </div>
        </div>
        <div className="d-flex gap-2">
          <CButton
            color="secondary"
            onClick={() => fetchUsers(true)}
            disabled={loading}
            title="Recargar listado"
          >
            <CIcon icon={cilReload} className="me-1" />
            Actualizar
          </CButton>
          <CButton color="primary" onClick={handleOpenCreateModal}>
            <CIcon icon={cilPlus} className="me-1" />
            Nuevo usuario
          </CButton>
        </div>
      </div>

      {feedback.message && (
        <CAlert
          color={feedback.type || 'info'}
          dismissible
          onClose={() => setFeedback({ type: '', message: '' })}
          className="mb-3"
        >
          {feedback.message}
        </CAlert>
      )}

      <CCard className="mb-4 shadow-sm">
        <CCardBody>
          <CRow className="g-3 align-items-center">
            <CCol md={7}>
              <CInputGroup>
                <CInputGroupText>
                  <CIcon icon={cilSearch} />
                </CInputGroupText>
                <CFormInput
                  placeholder="Buscar usuario por nombre o correo electrónico..."
                  value={searchTerm}
                  onChange={(event) => setSearchTerm(event.target.value)}
                />
              </CInputGroup>
            </CCol>
            <CCol md={3}>
              <CFormSelect
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value)}
                aria-label="Filtrar por estado"
              >
                <option value="ALL">Todos los estados</option>
                <option value="ACTIVE">Solo activos</option>
                <option value="INACTIVE">Solo inactivos</option>
              </CFormSelect>
            </CCol>
            <CCol md={2} className="text-md-end text-body-secondary small">
              Total: <strong>{filteredUsers.length}</strong> usuario(s)
            </CCol>
          </CRow>
        </CCardBody>
      </CCard>

      <CRow className="g-4">
        <CCol lg={8}>
          <CCard className="shadow-sm h-100">
            <CCardHeader className="fw-semibold">Listado de usuarios registrados</CCardHeader>
            <CCardBody className="p-0">
              <UsersTable
                users={filteredUsers}
                loading={loading}
                selectedUserId={selectedUserId}
                onSelectUser={(user) => setSelectedUserId(user.id)}
              />
            </CCardBody>
          </CCard>
        </CCol>

        <CCol lg={4}>
          <UserDetailCard
            userDetail={selectedUserDetail}
            userRoles={selectedUserRoles}
            loadingDetail={loadingDetail}
            onEditUser={handleOpenEditModal}
            onManageProfile={(user) => setProfileModalUser(user)}
            onManageRoles={(user) => setRolesModalUser(user)}
            onToggleStatus={handleToggleStatus}
            onDeleteUser={(user) => setUserToDelete(user)}
          />
        </CCol>
      </CRow>

      {/* Modal Crear / Editar Usuario */}
      <UserFormModal
        visible={formModalVisible}
        userToEdit={userToEdit}
        submitting={submittingForm}
        onClose={() => setFormModalVisible(false)}
        onSubmit={handleSubmitUserForm}
      />

      {/* Modal Gestionar Perfil del Usuario */}
      <UserProfileModal
        visible={Boolean(profileModalUser)}
        user={profileModalUser}
        onClose={() => setProfileModalUser(null)}
        onSaved={(message) => {
          showNotification('success', message)
          if (selectedUserId) {
            fetchUserDetail(selectedUserId)
          }
        }}
      />

      {/* Modal Gestionar Roles del Usuario */}
      <UserRolesModal
        visible={Boolean(rolesModalUser)}
        user={rolesModalUser}
        onClose={() => setRolesModalUser(null)}
        onRolesUpdated={() => {
          if (selectedUserId) {
            fetchUserDetail(selectedUserId)
          }
        }}
      />

      {/* Modal de Confirmación de Eliminación */}
      <CModal
        visible={Boolean(userToDelete)}
        onClose={() => setUserToDelete(null)}
        alignment="center"
      >
        <CModalHeader>
          <CModalTitle>Confirmar eliminación de usuario</CModalTitle>
        </CModalHeader>
        <CModalBody>
          <p className="mb-2">
            ¿Estás seguro de que deseas eliminar permanentemente al usuario{' '}
            <strong>{userToDelete?.name}</strong> ({userToDelete?.email})?
          </p>
          <div className="small text-body-secondary">
            Nota: Si solo deseas impedir su acceso temporalmente, puedes usar la opción de{' '}
            <strong>Desactivar</strong>. La eliminación no borrará los roles ni permisos existentes
            en el sistema.
          </div>
        </CModalBody>
        <CModalFooter>
          <CButton
            color="secondary"
            variant="outline"
            onClick={() => setUserToDelete(null)}
            disabled={deleting}
          >
            Cancelar
          </CButton>
          <CButton color="danger" className="text-white" onClick={handleConfirmDelete} disabled={deleting}>
            {deleting ? (
              <>
                <CSpinner size="sm" className="me-2" />
                Eliminando...
              </>
            ) : (
              'Sí, eliminar usuario'
            )}
          </CButton>
        </CModalFooter>
      </CModal>
    </div>
  )
}

export default UsersPage
