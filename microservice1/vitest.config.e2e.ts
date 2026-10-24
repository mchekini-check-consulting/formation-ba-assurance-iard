import { defineConfig } from 'vitest/config';
import tsconfigPaths from 'vite-tsconfig-paths';

export default defineConfig({
  plugins: [tsconfigPaths()],
  test: {
    globals: true,
    root: './',
    include: ['**/*.e2e-spec.ts'],
    // Démarrage du conteneur PostgreSQL (téléchargement de l'image compris)
    hookTimeout: 120_000,
  },
});
