// Appel de /actuator/health (Spring Boot Actuator) avec le token Keycloak,
// résultat ramené à un état affichable :
//   UP / DOWN : réponse du backend ({"status": "UP"} en 200, "DOWN" en 503)
//   REFUSE    : 401 (token absent, expiré ou invalide) ou 403 (token d'un
//               autre client), renvoyé par le gateway sans appeler le backend
//   ERREUR    : gateway injoignable, CORS refusé ou réponse inattendue
export type Etat = 'UP' | 'DOWN' | 'REFUSE' | 'ERREUR';

export interface Sante {
  etat: Etat;
  code: number; // code HTTP, 0 si pas de réponse
  detail: string;
}

export async function verifierSante(
  url: string,
  token: string,
  fetchFn: typeof fetch = fetch,
): Promise<Sante> {
  let reponse: Response;
  try {
    reponse = await fetchFn(url, { headers: { Authorization: `Bearer ${token}` } });
  } catch {
    return { etat: 'ERREUR', code: 0, detail: 'Gateway injoignable' };
  }
  const code = reponse.status;
  if (code === 401) {
    return { etat: 'REFUSE', code, detail: 'Token refusé par le gateway' };
  }
  if (code === 403) {
    return { etat: 'REFUSE', code, detail: "Token émis pour un autre client" };
  }
  const corps: { status?: unknown } = await reponse.json().catch(() => ({}));
  if (corps.status === 'UP') {
    return { etat: 'UP', code, detail: 'Backend opérationnel' };
  }
  if (corps.status === 'DOWN' || code === 503) {
    return { etat: 'DOWN', code, detail: 'Backend en difficulté' };
  }
  return { etat: 'ERREUR', code, detail: 'Réponse inattendue' };
}
