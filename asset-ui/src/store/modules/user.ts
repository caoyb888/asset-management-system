import { defineStore } from 'pinia'
import { login, getUserInfo, logout, type UserInfo, type LoginParams } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

interface UserState {
  token: string
  userInfo: UserInfo | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken(),
    userInfo: null,
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    nickname: (state) => state.userInfo?.nickname ?? '',
    roles: (state) => state.userInfo?.roles ?? [],
    permissions: (state) => state.userInfo?.permissions ?? [],
  },

  actions: {
    async login(params: LoginParams) {
      const result = await login(params)
      this.token = result.token
      setToken(result.token)
    },

    async getInfo() {
      const info = await getUserInfo()
      this.userInfo = info
      return info
    },

    async logout() {
      try {
        await logout()
      } finally {
        this.token = ''
        this.userInfo = null
        removeToken()
      }
    },

    resetState() {
      this.token = ''
      this.userInfo = null
      removeToken()
    },
  },
})
