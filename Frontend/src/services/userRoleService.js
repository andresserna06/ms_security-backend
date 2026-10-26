import { apiClient } from './apiClient'

export const userRoleService = {
  getAllRoles: () => apiClient.get('/roles'),

  getUserRoles: (userId) => apiClient.get(`/user-roles/users/${userId}`),

  assignRole: (userId, roleId) =>
    apiClient.post('/user-roles', {
      userId: Number(userId),
      roleId: Number(roleId),
    }),

  removeRole: (userId, roleId) => apiClient.delete(`/user-roles/${userId}/${roleId}`),
}

export default userRoleService
