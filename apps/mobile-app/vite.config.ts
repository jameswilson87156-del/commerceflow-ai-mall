import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_MOBILE_DEV_API_TARGET || 'http://127.0.0.1:8080'
  return {
    plugins: [uni()],
    resolve: {
      alias: [{
        find: /^vue$/,
        replacement: fileURLToPath(new URL('./src/uni-vue-compat.ts', import.meta.url))
      }]
    },
    server: {
      host: env.VITE_MOBILE_HOST || env.MOBILE_H5_HOST || '127.0.0.1',
      port: Number(env.VITE_MOBILE_PORT || env.MOBILE_H5_PORT || 5173),
      strictPort: true,
      proxy: {
        '/api': { target: apiTarget, changeOrigin: true }
      }
    }
  }
})
