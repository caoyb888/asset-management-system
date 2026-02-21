import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
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

// 请求拦截：注入 Token
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

// 响应拦截：统一错误处理
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { code, msg, data } = response.data
    if (code === 200) {
      return data as unknown as AxiosResponse
    }
    ElMessage.error(msg ?? '请求失败')
    return Promise.reject(new Error(msg))
  },
  (error) => {
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error(error.message ?? '网络异常')
    }
    return Promise.reject(error)
  },
)

export default request
