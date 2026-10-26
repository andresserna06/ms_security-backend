import { apiClient } from './apiClient'

export const profileService = {
  getByUserId: (userId) => apiClient.get(`/users/${userId}/profile`),

  create: (userId, payload) => apiClient.post(`/users/${userId}/profile`, payload),

  update: (userId, payload) => apiClient.put(`/users/${userId}/profile`, payload),

  remove: (userId) => apiClient.delete(`/users/${userId}/profile`),
}

export default profileService
