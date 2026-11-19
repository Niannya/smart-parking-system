import http from './http'

/** 本人车辆列表：后端按会话中的 owner 过滤，页面不传 owner ID */
export function listMyVehicles() {
  return http.get('/api/owner/vehicles')
}

/** 绑定车辆，data: { licensePlate } */
export function createVehicle(data) {
  return http.post('/api/owner/vehicles', data)
}

/** 只做启用/停用，不提供删除或换绑 */
export function changeVehicleStatus(id, status) {
  return http.patch(`/api/owner/vehicles/${id}/status`, { status })
}
