import http from './http'

export function getDashboardSummary({ lotId, date }) {
  return http.get('/api/dashboard/summary', { params: { lotId, date } })
}

export function getDashboardRevenue({ lotId, from, to }) {
  return http.get('/api/dashboard/revenue', { params: { lotId, from, to } })
}
