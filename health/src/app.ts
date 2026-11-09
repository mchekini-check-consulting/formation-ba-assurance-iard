import type Keycloak from 'keycloak-js';
import { verifierSante } from './health';

// Partie de la session keycloak-js utilisée (remplacée par un faux dans les tests)
export type Session = Pick<Keycloak, 'token' | 'tokenParsed' | 'updateToken' | 'login' | 'logout'>;

const GABARIT = `
  <section class="carte">
    <header>
      <h1>Santé de l'API Assurance IARD</h1>
      <p class="utilisateur"></p>
    </header>
    <p class="etat" data-etat="attente"><span class="pastille"></span><span class="libelle">…</span></p>
    <p class="detail"></p>
    <footer>
      <button type="button" class="verifier">Vérifier</button>
      <button type="button" class="deconnexion">Se déconnecter</button>
    </footer>
  </section>
`;

function element<T extends HTMLElement>(racine: HTMLElement, selecteur: string): T {
  return racine.querySelector(selecteur) as T;
}

// Affiche l'écran dans racine et lance une première vérification.
// Utilisateur connecté (garanti par main.ts) : chaque vérification rafraîchit
// le token si besoin, puis appelle healthUrl avec Authorization: Bearer.
export function demarrer(
  racine: HTMLElement,
  session: Session,
  healthUrl: string,
  fetchFn: typeof fetch = fetch,
): Promise<void> {
  racine.innerHTML = GABARIT;
  const etat = element(racine, '.etat');
  const libelle = element(racine, '.libelle');
  const detail = element(racine, '.detail');
  const verifier = element<HTMLButtonElement>(racine, '.verifier');
  const profil = session.tokenParsed;
  element(racine, '.utilisateur').textContent = String(
    profil?.['preferred_username'] ?? profil?.sub ?? '',
  );

  async function lancer(): Promise<void> {
    verifier.disabled = true;
    etat.dataset['etat'] = 'attente';
    libelle.textContent = '…';
    detail.textContent = '';
    try {
      await session.updateToken(30); // token valable encore 30 s au moins
    } catch {
      await session.login(); // session Keycloak expirée : reconnexion
      return;
    }
    const sante = await verifierSante(healthUrl, session.token ?? '', fetchFn);
    etat.dataset['etat'] = sante.etat.toLowerCase();
    libelle.textContent = sante.etat;
    const heure = new Date().toLocaleTimeString('fr-FR');
    detail.textContent = `${sante.detail}${sante.code ? ` · HTTP ${sante.code}` : ''} · ${heure}`;
    verifier.disabled = false;
  }

  verifier.addEventListener('click', () => void lancer());
  element(racine, '.deconnexion').addEventListener(
    'click',
    () => void session.logout({ redirectUri: location.origin }),
  );
  return lancer();
}
