import { defineConfig } from 'vitest/config'
import { loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_ADMIN_DEV_API_TARGET || `http://127.0.0.1:${env.MALL_API_PORT || 8080}`
  return {
  plugins: [vue()],
  server: { host: env.VITE_ADMIN_HOST || env.ADMIN_WEB_HOST || '127.0.0.1', port: Number(env.VITE_ADMIN_PORT || env.ADMIN_WEB_PORT || 5174), strictPort: true, proxy: { '/api': { target: apiTarget, changeOrigin: true } } },
  test: { environment: 'jsdom', globals: true },
  }
})
