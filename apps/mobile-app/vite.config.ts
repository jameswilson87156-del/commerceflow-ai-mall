import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: [{
      find: /^vue$/,
      replacement: fileURLToPath(new URL('./src/uni-vue-compat.ts', import.meta.url))
    }]
  }
})
