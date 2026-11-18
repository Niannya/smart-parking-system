// 会话本地缓存（真实鉴权在服务端会话/Cookie，这里只做路由分流与展示）
const KEY = 'parking_session'

export function loadSession() {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export function saveSession(session) {
  localStorage.setItem(KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(KEY)
}
