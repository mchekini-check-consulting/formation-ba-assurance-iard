import { defineConfig, devices } from '@playwright/test';

// Tests de bout en bout (make e2e-tests) : application construite (vite build)
// servie par vite preview, dans un vrai navigateur. Keycloak et le gateway sont
// simulés dans les tests (page.route) : aucun service extérieur, même résultat
// en local et en CI.
export default defineConfig({
  testDir: 'e2e',
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  // CI : rapport HTML (playwright-report/) publié en artifact en cas d'échec
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4201',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // PLAYWRIGHT_CHANNEL=chrome : Chrome déjà installé sur le poste, au lieu
        // du Chromium téléchargé par make e2e-tests
        channel: process.env['PLAYWRIGHT_CHANNEL'],
      },
    },
  ],
  webServer: {
    command: 'npm run build && npx vite preview --port 4201 --strictPort',
    url: 'http://localhost:4201',
    reuseExistingServer: !process.env['CI'],
  },
});
