import { clearSession } from './auth'

// 统一响应外壳：{ code, message, data }；code=0 成功，其余为稳定业务错误码
// 会话走 Cookie；写请求带 CSRF：先 GET /api/auth/csrf 拿 token，放 X-XSRF-TOKEN 头

let csrfToken = null

async function fetchCsrfToken() {
  const res = await fetch('/api/auth/csrf', { credentials: 'include' })
  const body = await res.json()
  if (body && body.code === 0 && body.data) {
    csrfToken = body.data.token
  }
  return csrfToken
}

function isWrite(method) {
  return method === 'POST' || method === 'PUT' || method === 'DELETE' || method === 'PATCH'
}

async function request(method, url, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (isWrite(method)) {
    if (!csrfToken) {
      await fetchCsrfToken()
    }
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken
    }
  }
  const res = await fetch(url, {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  // 401/403 统一清理本地会话并回登录页（安全框架返回 JSON）
  if (res.status === 401 || res.status === 403) {
    clearSession()
    if (!location.pathname.startsWith('/login')) {
      location.href = '/login'
    }
    const err = new Error('未登录或登录已过期')
    err.httpStatus = res.status
    throw err
  }
  let payload = null
  try {
    payload = await res.json()
  } catch (e) {
    const err = new Error('响应解析失败')
    err.httpStatus = res.status
    throw err
  }
  if (!payload || payload.code !== 0) {
    const err = new Error((payload && payload.message) || '请求失败')
    err.code = payload ? payload.code : 0
    throw err
  }
  return payload
}

const http = {
  get(url) {
    return request('GET', url)
  },
  post(url, body) {
    return request('POST', url, body === undefined ? {} : body)
  },
  put(url, body) {
    return request('PUT', url, body === undefined ? {} : body)
  },
  del(url) {
    return request('DELETE', url)
  }
}

export default http
