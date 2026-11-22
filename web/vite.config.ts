import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  base: './',
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api-lyceum': {
        target: 'https://lyceum.nstu.ru/info/api',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api-lyceum/, ''),
        secure: false,
      },
    },
  },
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      includeAssets: ['ic_lyceum_logo.png', 'ic_magma_logo.png', 'apple-touch-icon.png', 'icons/*.png'],
      manifest: {
        name: 'Лицей НГТУ — Расписание',
        short_name: 'ScheduleXY',
        description: 'Расписание занятий, звонков, заметки и свободные кабинеты Лицея НГТУ',
        theme_color: '#0F172A',
        background_color: '#0B0F17',
        display: 'standalone',
        orientation: 'portrait-primary',
        start_url: './#/',
        scope: './',
        icons: [
          {
            src: 'icons/icon-192.png',
            sizes: '192x192',
            type: 'image/png',
          },
          {
            src: 'icons/icon-512.png',
            sizes: '512x512',
            type: 'image/png',
          },
          {
            src: 'icons/icon-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any maskable',
          },
        ],
        shortcuts: [
          {
            name: 'Расписание',
            url: './#/schedule',
            icons: [{ src: 'icons/icon-192.png', sizes: '192x192' }],
          },
          {
            name: 'Календарь',
            url: './#/calendar',
            icons: [{ src: 'icons/icon-192.png', sizes: '192x192' }],
          },
          {
            name: 'Заметки и ДЗ',
            url: './#/notes',
            icons: [{ src: 'icons/icon-192.png', sizes: '192x192' }],
          },
          {
            name: 'Кабинеты',
            url: './#/freerooms',
            icons: [{ src: 'icons/icon-192.png', sizes: '192x192' }],
          },
          {
            name: 'Архив',
            url: './#/archive',
            icons: [{ src: 'icons/icon-192.png', sizes: '192x192' }],
          },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2,ttf}'],
      },
    }),
  ],
});
