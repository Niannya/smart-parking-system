import axios from 'axios'

const writeMethods = new Set(['post', 'put', 'patch', 'delete'])
let onUnauthorized = null

class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

const http = axios.create({
  withCredentials: true,
  xsrfCookieName: null,
  timeout: 8000,
})

http.interceptors.request.use(async (config) => {
  if (!config.url?.startsWith('/api/') || config.url.includes('\\')) {
    throw new ApiError('接口地址必须以 /api/ 开头')
  }

  if (writeMethods.has(config.method)) {
    // 新取会话令牌，避免登录或退出后重用旧 token；写操作失败不自动重发。
    const csrf = await http.get('/api/auth/csrf', { handleUnauthorized: false })
    if (!csrf?.token || csrf.headerName !== 'X-XSRF-TOKEN') {
      throw new ApiError('未取得有效的 CSRF 令牌，请重试。')
    }
    config.headers.set(csrf.headerName, csrf.token)
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (!body || typeof body.code !== 'number') {
      throw new ApiError('后端响应格式不正确', response.status)
    }
    if (body.code !== 0) {
      throw new ApiError(body.message || '操作失败', response.status, body.code)
    }
    return body.data
  },
  (error) => {
    if (error instanceof ApiError) return Promise.reject(error)
    const status = error.response?.status
    const body = error.response?.data
    const message = body?.message
      || (status === 403 ? '权限不足' : status ? '请求失败，请稍后重试' : '暂时无法连接后端，请稍后重试')
    if (status === 401 && error.config?.handleUnauthorized !== false) onUnauthorized?.()
    return Promise.reject(new ApiError(message, status, body?.code))
  },
)

export default http
