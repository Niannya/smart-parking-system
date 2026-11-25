import http from './http'

/**
 * 停车场下拉选项。
 *
 * <p>最终契约由成员 1 的停车场模块提供：{@code GET /api/parking-lots/options}。
 * PR #6 的临时接口 {@code /api/parking-areas/lots} 已按契约移除，页面统一走这里。</p>
 */
export function listLotOptions() {
  return http.get('/api/parking-lots/options')
}

/** 按停车场查询区域，params: { lotId, status } */
export function listAreas(params) {
  return http.get('/api/parking-areas', { params })
}

export function createArea(data) {
  return http.post('/api/parking-areas', data)
}

export function updateArea(id, data) {
  return http.put(`/api/parking-areas/${id}`, data)
}

export function changeAreaStatus(id, status) {
  return http.patch(`/api/parking-areas/${id}/status`, { status })
}

export function deleteArea(id) {
  return http.delete(`/api/parking-areas/${id}`)
}
