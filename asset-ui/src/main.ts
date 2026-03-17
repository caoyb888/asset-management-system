import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'

import App from './App.vue'
import router from './router'
import { setupDirectives } from './directives'
import './styles/index.scss'

// 在 Vue 挂载前立即恢复主题，避免闪烁
const savedTheme = localStorage.getItem('asset_theme')
if (savedTheme) {
  document.documentElement.dataset.theme = savedTheme
}

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

setupDirectives(app)

app.mount('#app')
