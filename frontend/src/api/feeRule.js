import http from '../http'

// 收费规则（成员 3，ADMIN）：/api/fee-rules

/** 每场规则历史：GET /api/fee-rules?lotId=1，按 createdAt DESC, id DESC */
export function listFeeRules(lotId) {
  return http.get(`/api/fee-rules?lotId=${encodeURIComponent(lotId)}`)
}

/** 当前生效规则：GET /api/fee-rules/active?lotId=1；无规则 404/4403 */
export function getActiveFeeRule(lotId) {
  return http.get(`/api/fee-rules/active?lotId=${encodeURIComponent(lotId)}`)
}

/**
 * 创建即激活：POST /api/fee-rules
 * 历史规则不可改删，只能新建；服务端会把旧活动规则转历史
 */
export function createFeeRule(payload) {
  return http.post('/api/fee-rules', {
    lotId: payload.lotId,
    ruleName: payload.ruleName,
    freeMinutes: payload.freeMinutes,
    billingUnitMinutes: payload.billingUnitMinutes,
    unitPrice: payload.unitPrice,
    maxFeePer24h: payload.maxFeePer24h
  })
}
