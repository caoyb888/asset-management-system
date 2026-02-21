import request from './request'
import { sm2Encrypt } from '@/utils/sm-crypto'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  tokenType: string
}

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
}

/** 登录（密码 SM2 加密后传输） */
export function login(params: LoginParams) {
  return request.post<LoginResult, LoginResult>('/auth/login', {
    username: params.username,
    password: sm2Encrypt(params.password),
  })
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return request.get<UserInfo, UserInfo>('/auth/userInfo')
}

/** 退出登录 */
export function logout() {
  return request.post('/auth/logout')
}
