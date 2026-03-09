import { defineStore } from 'pinia'
import {
  login,
  logout,
  getUserInfo,
  getUserRoutes,
  type UserInfo,
  type LoginParams,
  type RouteVO,
} from '@/api/auth'
import {
  getToken,
  setToken,
  removeToken,
  getRefreshToken,
  setRefreshToken,
} from '@/utils/auth'

interface UserState {
  token: string
  refreshToken: string
  userInfo: UserInfo | null
  routes: RouteVO[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token:        getToken(),
    refreshToken: getRefreshToken(),
    userInfo:     null,
    routes:       [],
  }),

  getters: {
    isLoggedIn:  (state) => !!state.token,
    nickname:    (state) => state.userInfo?.realName ?? state.userInfo?.nickname ?? '',
    roles:       (state) => state.userInfo?.roles ?? [],
    permissions: (state) => state.userInfo?.permissions ?? [],
  },

  actions: {
    async login(params: LoginParams) {
      const result = await login(params)
      // 后端返回字段名为 token，兼容 accessToken
      const accessToken = result.token || result.accessToken || ''
      const rt = result.refreshToken || ''
      this.token = accessToken
      this.refreshToken = rt
      setToken(accessToken)
      setRefreshToken(rt)
    },

    async getInfo() {
      const info = await getUserInfo()
      // 兼容旧字段 nickname
      if (!info.nickname) info.nickname = info.realName
      this.userInfo = info
      return info
    },

    async getRoutes() {
      const routes = await getUserRoutes()
      this.routes = routes
      return routes
    },

    async logout() {
      try {
        await logout(this.refreshToken || undefined)
      } finally {
        this.resetState()
      }
    },

    resetState() {
      this.token        = ''
      this.refreshToken = ''
      this.userInfo     = null
      this.routes       = []
      removeToken()
    },
  },
})
