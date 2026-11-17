import axios from 'axios'

/**
 * 全项目共用的 axios 实例。
 *
 * 两个约定：
 * 1. 后端地址只在这里出现一次，以后换端口/换机器只需要改 baseURL（或改成 vite 代理）；
 * 2. 后端统一响应结构是 { code, message, data }，code === 0 才算成功；
 *    失败时不区分 HTTP 状态，一律把 message 抛给页面展示，避免页面写一堆 if 判断。
 */
const http = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 8000,
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    // 有统一响应结构就取 data，没有就原样返回（兼容尚未改造的旧接口）
    return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body
  },
  (error) => {
    const body = error.response && error.response.data
    const message = (body && body.message) || '无法连接后端，请确认 8081 端口已启动'
    return Promise.reject(new Error(message))
  },
)

export default http
