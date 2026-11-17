import http from './http'

/** 按停车场/区域/状态查询车位，params: { lotId, areaId, status } */
export function listSpaces(params) {
  return http.get('/api/parking-spaces', { params })
}

export function createSpace(data) {
  return http.post('/api/parking-spaces', data)
}

export function updateSpace(id, data) {
  return http.put(`/api/parking-spaces/${id}`, data)
}

/** 只允许 FREE / DISABLED；OCCUPIED 由入场、离场事务维护 */
export function changeSpaceStatus(id, status) {
  return http.patch(`/api/parking-spaces/${id}/status`, { status })
}

export function deleteSpace(id) {
  return http.delete(`/api/parking-spaces/${id}`)
}
