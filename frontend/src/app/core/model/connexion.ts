export interface VerificationEnCours {
  verification: string;
  emailMasque: string;
}

export interface SessionOuverte {
  jeton: string;
  email: string;
}

export type MotifNouveauCode = 'essais-epuises' | 'code-expire';

export type ResultatConnexion =
  | { statut: 'code-envoye' }
  | { statut: 'identifiants-incorrects' }
  | { statut: 'compte-bloque'; bloqueJusquA: string }
  | { statut: 'erreur-technique' };

export type ResultatCode =
  | { statut: 'connecte' }
  | { statut: 'code-incorrect'; essaisRestants: number }
  | { statut: 'nouveau-code-requis'; motif: MotifNouveauCode }
  | { statut: 'verification-perdue' }
  | { statut: 'erreur-technique' };

export type ResultatRenvoi = 'code-renvoye' | 'verification-perdue' | 'erreur-technique';

export const PAGE_APRES_CONNEXION = '/espace-client';

export function codeValide(code: string): boolean {
  return /^\d{6}$/.test(code);
}

/** Page où revenir après la connexion : uniquement une adresse interne au site. */
export function pageDeRetour(retour: string | null | undefined): string {
  return retour && /^\/(?![/\\])/.test(retour) ? retour : PAGE_APRES_CONNEXION;
}
