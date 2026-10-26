import { apiClient } from './apiClient'

export const userService = {
  getAll: () => apiClient.get('/users'),

  getById: (id) => apiClient.get(`/users/${id}`),

  getDetailWithProfile: (id) => apiClient.get(`/users/${id}/detail`),

  getDetailWithRoles: (id) => apiClient.get(`/users/${id}/detail-with-roles`),

  getDetailWithSessions: (id) => apiClient.get(`/users/${id}/detail-with-sessions`),

  create: (payload) => apiClient.post('/users', payload),

  update: (id, payload) => apiClient.put(`/users/${id}`, payload),

  activate: (id) => apiClient.patch(`/users/${id}/activate`),

  deactivate: (id) => apiClient.patch(`/users/${id}/deactivate`),

  remove: (id) => apiClient.delete(`/users/${id}`),
}

export default userService
