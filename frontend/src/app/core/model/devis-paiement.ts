import { JourPrelevement } from './premier-prelevement';

/** Étape 5, gardée en mémoire. L'autorisation de prélèvement n'en fait pas partie (RG5). */
export interface SaisiePaiement {
  dateDebut: string;
  titulaire: string;
  iban: string;
  jour: JourPrelevement | null;
}

export function paiementVide(): SaisiePaiement {
  return { dateDebut: '', titulaire: '', iban: '', jour: null };
}
