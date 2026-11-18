import http from '../http'

// 停车记录查询（成员 3，ADMIN）：/api/parking-records

/**
 * 管理员列表：GET /api/parking-records
 * lotId/licensePlate/status/spaceId/from/to 均可省；from/to 应同时提供；
 * 按 entryTime DESC, id DESC；空串过滤值 400
 */
export function listRecords(params) {
  const query = params || {}
  const search = new URLSearchParams()
  if (query.lotId) {
    search.set('lotId', query.lotId)
  }
  if (query.licensePlate) {
    search.set('licensePlate', query.licensePlate)
  }
  if (query.status) {
    search.set('status', query.status)
  }
  if (query.spaceId) {
    search.set('spaceId', query.spaceId)
  }
  if (query.from) {
    search.set('from', query.from)
  }
  if (query.to) {
    search.set('to', query.to)
  }
  const suffix = search.toString()
  return http.get(suffix ? `/api/parking-records?${suffix}` : '/api/parking-records')
}

/** 管理员详情：GET /api/parking-records/{id}；不存在 404/4601 */
export function getRecord(id) {
  return http.get(`/api/parking-records/${encodeURIComponent(id)}`)
}
