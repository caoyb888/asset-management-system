import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 3100,
    proxy: {
      // /file/* → asset-file (8012)：文件上传接口 + 静态文件访问
      '/file': {
        target: 'http://localhost:8012',
        changeOrigin: true,
      },
      // /api/auth/* → asset-system (8006) - 认证接口，必须在 /api 规则之前
      '/api/auth': {
        target: 'http://localhost:8006',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // /api/sys/* → asset-system (8006) - 必须在 /api 规则之前
      '/api/sys': {
        target: 'http://localhost:8006',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // /api/fin/* → asset-finance (8004) - 必须在 /api 规则之前
      '/api/fin': {
        target: 'http://localhost:8004',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // /api/opr/* → asset-operation (8003) - 必须在 /api 规则之前
      '/api/opr': {
        target: 'http://localhost:8003',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // /api/inv/* → asset-investment (8002) - 必须在 /api 规则之前
      '/api/inv': {
        target: 'http://localhost:8002',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // /api/rpt/* → asset-report (8005) - 必须在 /api 规则之前
      '/api/rpt': {
        target: 'http://localhost:8005',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // /api/* → asset-base (8001)
      '/api': {
        target: 'http://localhost:8001',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  build: {
    target: 'es2022',
    chunkSizeWarningLimit: 2000,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          elementPlus: ['element-plus'],
          echarts: ['echarts'],
          three: ['three'],
        },
      },
    },
  },
})
