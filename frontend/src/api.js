// URL d'un backend du projet (par son nom dans helm.yaml), déduite du host
// du frontend :
//   https://<application>.<projet>.<env>.<domaine>
//   -> passerelle https://gateway.<projet>.<env>.<domaine>/<backend>
// Même image dans tous les environnements (promotion Kargo) : rien à configurer.
// En local (localhost) : /api/<backend>, relayé vers le backend par le - 
// serveur de dev (server.proxy de vite.config.js).
export function apiBase(backend, hostname = window.location.hostname) {
  const [, projet, env, ...domaine] = hostname.split('.')
  if (!projet || !env || domaine.length === 0) {
    return `/api/${backend}`
  }
  return `https://gateway.${projet}.${env}.${domaine.join('.')}/${backend}`
}

async function appeler(chemin, options, base) {
  const reponse = await fetch(`${base}${chemin}`, options)
  if (!reponse.ok) {
    throw new Error(`HTTP ${reponse.status}`)
  }
  return reponse.json()
}

// Ressource générique du backend : GET /items
export function listerItems(base = apiBase('backend')) {
  return appeler('/items', undefined, base)
}

// POST /items {nom, description}
export function creerItem(item, base = apiBase('backend')) {
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

// Backend contrats : GET /contrats
export function listerContrats(base = apiBase('contrats')) {
  return appeler('/contrats', undefined, base)
}

// POST /contrats {numero, assure}
export function creerContrat(contrat, base = apiBase('contrats')) {
  return appeler(
    '/contrats',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(contrat),
    },
    base,
  )
}

// Vérifie que la passerelle route bien vers microservice1 (squelette minimal,
// GET / -> texte brut "Hello World!", pas de JSON).
export async function saluerMicroservice1(base = apiBase('microservice1')) {
  const reponse = await fetch(base)
  if (!reponse.ok) {
    throw new Error(`HTTP ${reponse.status}`)
  }
  return reponse.text()
}
