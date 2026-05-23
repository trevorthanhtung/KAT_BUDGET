import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'KAT Budget',
        short_name: 'KAT Budget',
        description: 'Quan ly tai chinh ca nhan cho gia dinh.',
        theme_color: '#0f766e',
        background_color: '#f7f3ea',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        icons: [
          {
            src: '/icon-192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'any maskable',
          },
          {
            src: '/icon-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any maskable',
          },
        ],
      },
      workbox: {
        navigateFallback: '/index.html',
      },
      devOptions: {
        enabled: true,
      },
    }),
  ],
  server: {
    proxy: {
      '/api/vcb-rates': {
        target: 'https://portal.vietcombank.com.vn',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/vcb-rates/, '/Usercontrols/TVPortal.TyGia/pXML.aspx')
      }
    }
  }
})
