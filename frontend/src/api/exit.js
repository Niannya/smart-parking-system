import http from '../http'

// 离场与模拟支付（成员 3，ADMIN）：/api/parking-exits

/**
 * 查在场记录：GET /api/parking-exits/active
 * licensePlate 与 recordNo 二者恰填一个；车牌须规范化；无则 404/4501
 */
export function findActiveRecord({ licensePlate, recordNo }) {
  const params = new URLSearchParams()
  if (licensePlate) {
    params.set('licensePlate', licensePlate)
  }
  if (recordNo) {
    params.set('recordNo', recordNo)
  }
  return http.get(`/api/parking-exits/active?${params.toString()}`)
}

/** 费用预览：GET /api/parking-exits/{recordId}/quote；只接受 PARKING，金额不锁定 */
export function getQuote(recordId) {
  return http.get(`/api/parking-exits/${encodeURIComponent(recordId)}/quote`)
}

/**
 * 正常离场 + 模拟确认收款：POST /api/parking-exits/{recordId}
 * paymentMethod: CASH | ELECTRONIC（实收=应收）| FREE_RELEASE（实收 0 且 remark 必填）
 * 服务端按当时时刻重算，实收与应收不一致 4503；重复 POST 409/4502
 */
export function exitRecord(recordId, payload) {
  return http.post(`/api/parking-exits/${encodeURIComponent(recordId)}`, {
    paymentMethod: payload.paymentMethod,
    amountPaid: payload.amountPaid,
    remark: payload.remark === undefined ? null : payload.remark
  })
}

/** 异常离场：POST /api/parking-exits/{recordId}/abnormal；原因必填，不生成支付记录 */
export function abnormalExit(recordId, reason) {
  return http.post(`/api/parking-exits/${encodeURIComponent(recordId)}/abnormal`, { reason })
}
