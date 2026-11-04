import { expect, test, type Page } from '@playwright/test';

// Page servie sur localhost : Keycloak de dev et santé par /api (src/config.ts)
const KEYCLOAK =
  'https://keycloak.dev.check-consulting.net/realms/check-projects-sso/protocol/openid-connect';
const SANTE = '**/api/actuator/health';

// JWT factice, non signé : la signature n'est vérifiée que par le gateway,
// simulé ici. keycloak-js ne fait que lire son contenu.
function jwt(claims: Record<string, unknown>): string {
  const b64 = (valeur: unknown) => Buffer.from(JSON.stringify(valeur)).toString('base64url');
  return `${b64({ alg: 'none', typ: 'JWT' })}.${b64(claims)}.signature`;
}

const maintenant = Math.floor(Date.now() / 1000);
const TOKEN = jwt({
  sub: 'id-1',
  preferred_username: 'alice',
  azp: 'assureasy-frontend-client',
  iat: maintenant,
  exp: maintenant + 3600,
});

// Keycloak simulé : la page de connexion renvoie aussitôt vers l'application
// avec un code (comme après saisie du mot de passe), le code est échangé
// contre TOKEN, la déconnexion affiche une page vide.
async function simulerKeycloak(page: Page): Promise<void> {
  await page.route(`${KEYCLOAK}/auth**`, (route) => {
    const demande = new URL(route.request().url());
    const retour =
      `${demande.searchParams.get('redirect_uri')}` +
      `#state=${demande.searchParams.get('state')}&session_state=s&code=code-factice`;
    return route.fulfill({
      contentType: 'text/html',
      body: `<script>location.replace(${JSON.stringify(retour)})</script>`,
    });
  });
  await page.route(`${KEYCLOAK}/token`, (route) =>
    route.fulfill({
      contentType: 'application/json',
      // keycloak-js envoie les cookies (credentials: include) : origine exacte,
      // comme les Web origins du client dans Keycloak
      headers: {
        'access-control-allow-origin': 'http://localhost:4201',
        'access-control-allow-credentials': 'true',
      },
      body: JSON.stringify({
        access_token: TOKEN,
        refresh_token: jwt({ exp: maintenant + 7200 }),
        token_type: 'Bearer',
        expires_in: 3600,
      }),
    }),
  );
  await page.route(`${KEYCLOAK}/logout**`, (route) =>
    route.fulfill({ contentType: 'text/html', body: '<p>Déconnecté</p>' }),
  );
}

// Gateway simulé : réponses successives de /backend/actuator/health ; renvoie
// les en-têtes Authorization reçus
async function simulerSante(page: Page, ...reponses: [number, unknown][]): Promise<string[]> {
  const authorizations: string[] = [];
  await page.route(SANTE, (route) => {
    authorizations.push(route.request().headers()['authorization'] ?? '');
    const [status, corps] = reponses[Math.min(authorizations.length, reponses.length) - 1];
    return route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(corps) });
  });
  return authorizations;
}

test.beforeEach(async ({ page }) => {
  await simulerKeycloak(page);
});

test('se connecte par Keycloak et affiche UP, avec le token envoyé au gateway', async ({
  page,
}) => {
  const authorizations = await simulerSante(page, [200, { status: 'UP' }]);
  await page.goto('/');
  await expect(page.locator('.libelle')).toHaveText('UP');
  await expect(page.locator('.utilisateur')).toHaveText('alice');
  await expect(page.locator('.detail')).toContainText('HTTP 200');
  expect(authorizations).toEqual([`Bearer ${TOKEN}`]);
  // Paramètres de retour de Keycloak retirés de l'adresse
  expect(page.url()).toBe('http://localhost:4201/');
});

test('affiche REFUSE quand le gateway rejette le token', async ({ page }) => {
  await simulerSante(page, [401, {}]);
  await page.goto('/');
  await expect(page.locator('.libelle')).toHaveText('REFUSE');
  await expect(page.locator('.detail')).toContainText('Token refusé par le gateway');
});

test('relance la vérification au clic sur Vérifier', async ({ page }) => {
  const authorizations = await simulerSante(page, [503, { status: 'DOWN' }], [200, { status: 'UP' }]);
  await page.goto('/');
  await expect(page.locator('.libelle')).toHaveText('DOWN');
  await page.getByRole('button', { name: 'Vérifier' }).click();
  await expect(page.locator('.libelle')).toHaveText('UP');
  expect(authorizations).toHaveLength(2);
});

test('se déconnecte par Keycloak et revient sur la page', async ({ page }) => {
  await simulerSante(page, [200, { status: 'UP' }]);
  await page.goto('/');
  await expect(page.locator('.libelle')).toHaveText('UP');
  const deconnexion = page.waitForRequest(`${KEYCLOAK}/logout**`);
  await page.getByRole('button', { name: 'Se déconnecter' }).click();
  const url = new URL((await deconnexion).url());
  expect(url.searchParams.get('post_logout_redirect_uri')).toBe('http://localhost:4201');
  expect(url.searchParams.get('client_id')).toBe('assureasy-frontend-client');
});
