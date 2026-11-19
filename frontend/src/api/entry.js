import http from './http'

/** 入场页只消费成员 1 的停车场 options 契约，不自己写停车场请求逻辑 */
export function listParkingLotOptions() {
  return http.get('/api/parking-lots/options')
}

/** 管理员办理入场，data: { lotId, licensePlate } */
export function createEntry(data) {
  return http.post('/api/parking-entries', data)
}
