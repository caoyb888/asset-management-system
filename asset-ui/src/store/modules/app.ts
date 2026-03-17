import { defineStore } from 'pinia'

const THEME_STORAGE_KEY = 'asset_theme'

interface AppState {
  sidebarCollapsed: boolean
  pageTitle: string
  theme: string
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    sidebarCollapsed: false,
    pageTitle: '工作台',
    theme: localStorage.getItem(THEME_STORAGE_KEY) || '',
  }),

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    setPageTitle(title: string) {
      this.pageTitle = title
    },
    setTheme(theme: string) {
      this.theme = theme
      if (theme) {
        document.documentElement.dataset.theme = theme
        localStorage.setItem(THEME_STORAGE_KEY, theme)
      } else {
        delete document.documentElement.dataset.theme
        localStorage.removeItem(THEME_STORAGE_KEY)
      }
    },
  },
})
