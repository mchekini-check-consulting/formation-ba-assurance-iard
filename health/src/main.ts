import Keycloak from 'keycloak-js';
import { demarrer } from './app';
import { CLIENT_ID, REALM, config } from './config';
import './style.css';

// Connexion obligatoire : sans session, redirection vers la page de connexion
// du realm Keycloak, puis retour ici (client public, code + PKCE). Le token
// obtenu est envoyé au gateway, qui le vérifie (middleware jwt).
const { keycloakUrl, healthUrl } = config();
const keycloak = new Keycloak({ url: keycloakUrl, realm: REALM, clientId: CLIENT_ID });

keycloak
  .init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
  .then(() => demarrer(document.getElementById('app') as HTMLElement, keycloak, healthUrl))
  .catch(() => {
    (document.getElementById('app') as HTMLElement).textContent = 'Keycloak injoignable';
  });
