import http from './http'

const lotsPath = '/api/parking-lots'

export function listParkingLots(status) {
  return http.get(lotsPath, { params: status ? { status } : undefined })
}

export function listParkingLotOptions() {
  return http.get(`${lotsPath}/options`)
}

export function createParkingLot(payload) {
  return http.post(lotsPath, payload)
}

export function updateParkingLot(id, payload) {
  return http.put(`${lotsPath}/${id}`, payload)
}

export function setParkingLotStatus(id, status) {
  return http.patch(`${lotsPath}/${id}/status`, { status })
}

export function deleteParkingLot(id) {
  return http.delete(`${lotsPath}/${id}`)
}
