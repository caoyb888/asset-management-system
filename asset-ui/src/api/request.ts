import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, setToken, getRefreshToken, removeToken } from '@/utils/auth'
import router from '@/router'

export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// ─── 请求拦截：注入 Access Token ──────────────────────────────────────────────
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// ─── 响应拦截：统一错误处理 + 401 自动刷新 ────────────────────────────────────
let isRefreshing = false
let pendingQueue: Array<(token: string) => void> = []

request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // blob 响应（文件下载）：直接返回原始数据
    if (response.config.responseType === 'blob') {
      return response.data as unknown as AxiosResponse
    }
    const { code, msg, data } = response.data
    if (code === 200) {
      return data as unknown as AxiosResponse
    }
    ElMessage.error(msg ?? '请求失败')
    return Promise.reject(new Error(msg))
  },
  async (error) => {
    const status = error.response?.status
    const originalConfig = error.config

    // 401：尝试用 Refresh Token 刷新 Access Token
    if (status === 401 && !originalConfig._retried) {
      const rt = getRefreshToken()
      if (rt) {
        originalConfig._retried = true
        if (!isRefreshing) {
          isRefreshing = true
          try {
            // 动态 import 避免循环依赖
            const { refreshToken } = await import('@/api/auth')
            const result = await refreshToken(rt)
            const newToken = result.accessToken
            setToken(newToken)
            // 通知等待队列
            pendingQueue.forEach(cb => cb(newToken))
            pendingQueue = []
            // 重试原始请求
            originalConfig.headers.Authorization = `Bearer ${newToken}`
            return request(originalConfig)
          } catch {
            // 刷新失败：清除登录状态跳转登录页
            removeToken()
            pendingQueue = []
            ElMessage.error('登录已过期，请重新登录')
            router.push('/login')
          } finally {
            isRefreshing = false
          }
        } else {
          // 其它请求排队等待刷新完成
          return new Promise(resolve => {
            pendingQueue.push((token: string) => {
              originalConfig.headers.Authorization = `Bearer ${token}`
              resolve(request(originalConfig))
            })
          })
        }
      } else {
        ElMessage.error('登录已过期，请重新登录')
        router.push('/login')
      }
    } else if (status !== 401) {
      ElMessage.error(error.message ?? '网络异常')
    }

    return Promise.reject(error)
  },
)

export default request
