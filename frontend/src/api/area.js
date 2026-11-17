import http from './http'

/** 停车场下拉选项（临时接口，等停车场模块合并后改为调用停车场列表接口） */
export function listLotOptions() {
  return http.get('/api/parking-areas/lots')
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
