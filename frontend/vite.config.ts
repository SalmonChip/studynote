import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
export default defineConfig(async ({ mode }) => ({
  plugins: [vue(), ...(mode === 'smoke' ? [(await import('./tests/mock-api')).mockApi()] : [])],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: { host: '127.0.0.1', proxy: mode === 'smoke' ? undefined : { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true } } },
  test: { environment: 'jsdom', clearMocks: true },
}))
