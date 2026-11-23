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
        name: 'Инженерный лицей НГТУ',
        short_name: 'Инженерный лицей НГТУ',
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
            icons: [{ src: 'icons/shortcut-schedule.png', sizes: '192x192', type: 'image/png' }],
          },
          {
            name: 'Календарь',
            url: './#/calendar',
            icons: [{ src: 'icons/shortcut-calendar.png', sizes: '192x192', type: 'image/png' }],
          },
          {
            name: 'Заметки и ДЗ',
            url: './#/notes',
            icons: [{ src: 'icons/shortcut-notes.png', sizes: '192x192', type: 'image/png' }],
          },
          {
            name: 'Кабинеты',
            url: './#/freerooms',
            icons: [{ src: 'icons/shortcut-rooms.png', sizes: '192x192', type: 'image/png' }],
          },
          {
            name: 'Архив',
            url: './#/archive',
            icons: [{ src: 'icons/shortcut-archive.png', sizes: '192x192', type: 'image/png' }],
          },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2,ttf}'],
      },
    }),
  ],
});
