import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Appels aux backends en local : /api/<backend> -> backend lancé sur le
    // poste, chacun sur son port (src/api.js).
    proxy: {
      '/api/backend': {
        target: 'http://localhost:3000',
        rewrite: (path) => path.replace(/^\/api\/backend/, ''),
      },
    },
  },
  // Tests unitaires (vitest) : DOM simulé par jsdom
  test: {
    environment: 'jsdom',
    coverage: {
      include: ['src/**/*.{js,jsx}'], // pas les css / images importés
    },
  },
})
