import { DocumentClient } from './document';

/** US-04 : un client dans la liste du back-office. */
export interface ClientResume {
  id: number;
  prenom: string | null;
  nom: string | null;
  email: string;
  profil: string;
}

/** AC4 : de l'IBAN, le serveur n'envoie que les 4 derniers caractères (ibanFin). */
export interface ContratClient {
  referenceDevis: string;
  numero: string;
  statut: string;
  adresseLogement: string;
  codePostal: string;
  ville: string;
  cotisationMensuelle: number;
  dateEffet: string;
  titulaire: string | null;
  ibanFin: string | null;
  jourPrelevement: number | null;
  rum: string | null;
}

export interface DossierClient {
  id: number;
  civilite: string | null;
  prenom: string | null;
  nom: string | null;
  email: string;
  telephone: string | null;
  profil: string;
  contrats: ContratClient[];
  documents: DocumentClient[];
}

/** RG3 : une tentative d'accès refusée. */
export interface AccesRefuse {
  date: string;
  email: string;
  profil: string;
  methode: string;
  chemin: string;
  ressource: string;
  identifiant: string | null;
  rubrique: string;
  droit: string;
  motif: string;
}

export const LIBELLES_MOTIF: Record<string, string> = {
  DROIT_ABSENT: 'Droit non accordé au profil',
  PAS_PROPRIETAIRE: "Donnée d'un autre client",
  BACK_OFFICE: 'Back-office demandé par un client',
};

export function nomComplet(personne: { prenom: string | null; nom: string | null; email: string }): string {
  const nom = [personne.prenom, personne.nom].filter(Boolean).join(' ');
  return nom || personne.email;
}
