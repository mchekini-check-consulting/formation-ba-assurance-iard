// US-04 : droits de l'US-02 fournis par le serveur avec la session.

export type Rubrique =
  | 'DEVIS'
  | 'SOUSCRIPTION'
  | 'CONTRATS'
  | 'DOCUMENTS'
  | 'SINISTRES'
  | 'COMPTES_UTILISATEURS'
  | 'PROFILS';

export type Droit = 'CONSULTER' | 'CREER' | 'MODIFIER' | 'DESACTIVER';

export interface SessionCourante {
  email: string;
  profil: string;
  droits?: string[];
}

/** Profils du back-office : un assuré ne le voit jamais. */
export const PROFILS_INTERNES: readonly string[] = ['GESTIONNAIRE', 'ADMINISTRATEUR'];

export const PAGE_ACCES_NON_AUTORISE = '/acces-non-autorise';
