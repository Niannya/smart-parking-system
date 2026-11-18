import http from './http'

export function registerAccount(payload) {
  return http.post('/api/auth/register', payload)
}

export function loginAccount(payload) {
  return http.post('/api/auth/login', payload, { handleUnauthorized: false })
}

export function getCurrentAccount() {
  return http.get('/api/auth/me', { handleUnauthorized: false })
}

export function logoutAccount() {
  return http.post('/api/auth/logout')
}

export function listUsers(filters) {
  return http.get('/api/users', { params: filters })
}

export function setUserStatus(id, status) {
  return http.patch(`/api/users/${id}/status`, { status })
}
