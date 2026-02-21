import { defineStore } from 'pinia'

interface AppState {
  sidebarCollapsed: boolean
  pageTitle: string
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    sidebarCollapsed: false,
    pageTitle: '工作台',
  }),

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    setPageTitle(title: string) {
      this.pageTitle = title
    },
  },
})
