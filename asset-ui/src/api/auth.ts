import request from './request'
import { sm2Encrypt } from '@/utils/sm-crypto'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  /** JWT Access Token（30分钟有效）*/
  accessToken: string
  /** Refresh Token（UUID，7天有效，存 localStorage）*/
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  nickname?: string   // 兼容旧字段，等于 realName
  avatar: string
  deptId?: number
  deptName?: string
  roles: string[]
  permissions: string[]
}

export interface RouteVO {
  path: string
  name: string
  component: string
  meta: { title: string; icon: string }
  children?: RouteVO[]
}

/** 登录（密码 SM2 加密后传输） */
export function login(params: LoginParams): Promise<LoginResult> {
  return request.post<LoginResult, LoginResult>('/auth/login', {
    username: params.username,
    password: sm2Encrypt(params.password),
  })
}

/** 刷新 Access Token */
export function refreshToken(refreshToken: string): Promise<LoginResult> {
  return request.post<LoginResult, LoginResult>('/auth/refresh', { refreshToken })
}

/** 获取当前用户信息（权限 + 角色） */
export function getUserInfo(): Promise<UserInfo> {
  return request.get<UserInfo, UserInfo>('/auth/info')
}

/** 获取当前用户动态路由 */
export function getUserRoutes(): Promise<RouteVO[]> {
  return request.get<RouteVO[], RouteVO[]>('/auth/user-routes')
}

/** 退出登录 */
export function logout(rt?: string) {
  return request.post('/auth/logout', rt ? { refreshToken: rt } : {})
}
