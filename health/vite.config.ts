import { defineConfig } from 'vitest/config';

export default defineConfig({
  server: {
    port: 4201,
    // En local : /api -> backend lancé sur le poste (pas de gateway, donc pas
    // de vérification du token), préfixe retiré (src/config.ts)
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  // Tests unitaires (vitest) : DOM simulé par jsdom
  test: {
    environment: 'jsdom',
    coverage: {
      include: ['src/**/*.ts'],
      // Démarrage / câblage Keycloak, sans logique à tester
      exclude: ['src/main.ts', 'src/**/*.spec.ts'],
      reporter: ['text', 'lcov'], // -> coverage/lcov.info (SonarQube)
    },
  },
});
