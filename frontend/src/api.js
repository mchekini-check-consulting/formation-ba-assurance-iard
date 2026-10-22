// URL du backend du projet, déduite du host du frontend :
//   https://<application>.<projet>.<env>.<domaine>
//   -> passerelle https://api.<env>.<domaine>/<projet>/backend
// Même image dans tous les environnements (promotion Kargo) : rien à configurer.
// En local (localhost) : /api, relayé vers le backend par le serveur de dev
// (server.proxy de vite.config.js).
export function apiBase(hostname = window.location.hostname) {
  const [, projet, env, ...domaine] = hostname.split('.')
  if (!projet || !env || domaine.length === 0) {
    return '/api'
  }
  return `https://api.${env}.${domaine.join('.')}/${projet}/backend`
}

async function appeler(chemin, options, base) {
  const reponse = await fetch(`${base}${chemin}`, options)
  if (!reponse.ok) {
    throw new Error(`HTTP ${reponse.status}`)
  }
  return reponse.json()
}

// Ressource générique du backend : GET /items
export function listerItems(base = apiBase()) {
  return appeler('/items', undefined, base)
}

// POST /items {nom, description}
export function creerItem(item, base = apiBase()) {
  return appeler(
    '/items',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(item),
    },
    base,
  )
}
