import http from '../http'

// 车主本人的停车记录（成员 3，OWNER）：/api/owner/parking-records
// 服务端按会话 JOIN vehicle.owner_user_id，不接受客户端传 owner ID

/** 本人列表：GET /api/owner/parking-records?status=PARKING（status 可省） */
export function listOwnRecords(status) {
  const suffix = status ? `?status=${encodeURIComponent(status)}` : ''
  return http.get(`/api/owner/parking-records${suffix}`)
}

/** 本人详情：GET /api/owner/parking-records/{id}；非本人 403/4602 */
export function getOwnRecord(id) {
  return http.get(`/api/owner/parking-records/${encodeURIComponent(id)}`)
}
