// Adresses déduites du host de la page (comme apiBase du frontend) :
//   https://health.<projet>.<env>.<domaine>
//   -> Keycloak : https://keycloak.<env>.<domaine>, realm check-projects-sso
//   -> santé    : https://gateway.<projet>.<env>.<domaine>/backend/actuator/health
//                 (route protégée par le middleware jwt de gateway.yaml)
// Même image dans tous les environnements (promotion Kargo) : rien à configurer.
// En local (localhost) : Keycloak de dev (http://localhost:4201/* doit être une
// redirection autorisée du client) et santé par /api, relayé vers le backend
// du poste par le serveur de dev (vite.config.ts).
export const REALM = 'check-projects-sso';
export const CLIENT_ID = 'assureasy-frontend-client';
const KEYCLOAK_LOCAL = 'https://keycloak.dev.check-consulting.net';

export interface Config {
  keycloakUrl: string;
  healthUrl: string;
}

export function config(hostname: string = location.hostname): Config {
  const [, projet, env, ...domaine] = hostname.split('.');
  if (!projet || !env || domaine.length === 0) {
    return { keycloakUrl: KEYCLOAK_LOCAL, healthUrl: '/api/actuator/health' };
  }
  const suffixe = `${env}.${domaine.join('.')}`;
  return {
    keycloakUrl: `https://keycloak.${suffixe}`,
    healthUrl: `https://gateway.${projet}.${suffixe}/backend/actuator/health`,
  };
}
