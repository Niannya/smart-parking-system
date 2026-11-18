import http from '../http'

// 场地选项（成员 1 scaffold 提供）：GET /api/parking-lots/options
// 只取 ACTIVE，按 lotCode 升序，返回 [{ id, lotCode, lotName }]

export function listLotOptions() {
  return http.get('/api/parking-lots/options')
}
