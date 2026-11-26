import type { DocumentContrat, Produit, StatutContrat } from './contrat';

/** US-23 : garantie ajoutée ou retirée par un avenant, avec son prix mensuel TTC. */
export interface GarantieAvenant {
  code: string;
  libelle: string;
  prixMensuelTtc: number;
}

/** US-23 : un avenant du contrat (historique de la fiche, avenant en attente). */
export interface Avenant {
  /** AV-AAAA-NNNNNN. */
  numero: string;
  /** Rang de l'avenant dans le contrat (1, 2, 3…). */
  rang: number;
  statut: StatutAvenant;
  /** Lendemain de l'acceptation (pour un avenant en attente : s'il est accepté aujourd'hui). */
  dateEffet: string;
  ancienneCotisationMensuelle: number;
  nouvelleCotisationMensuelle: number;
  ajouts: GarantieAvenant[];
  retraits: GarantieAvenant[];
  accepteLe: string | null;
  /** PDF de l'avenant (téléchargeable par /documents-contrat/{id}) ; null avant l'acceptation. */
  documentId: number | null;
}

export type StatutAvenant = 'EN_ATTENTE' | 'ACCEPTE' | 'ABANDONNE';

/** US-23, AC3 : ce qui change et les documents à relire avant d'accepter. */
export interface ProjetAvenant {
  avenant: Avenant;
  ancienneCotisationAnnuelle: number;
  nouvelleCotisationAnnuelle: number;
  /** MENSUEL ou ANNUEL. */
  frequencePaiement: string | null;
  /** RG3 : première échéance à la nouvelle cotisation ; null si le contrat n'a plus d'échéance. */
  premiereEcheanceConcernee: string | null;
  /** RG4 : vrai si une garantie est retirée. */
  reductionCouverture: boolean;
  /** RG4 : conseil mis à jour quand la couverture diminue. */
  conseils: string[];
  documents: DocumentContrat[];
  /** Auto : formule dont l'IPID se télécharge par /documents-contrat/ipid/{formule}. */
  ipidFormule: string | null;
}

/** US-23, AC1 et AC2 : une garantie du contrat, ou une option qu'on peut lui ajouter. */
export interface GarantieProposee {
  code: string;
  libelle: string;
  prixMensuelTtc: number | null;
  souscrite: boolean;
  obligatoire: boolean;
  modifiable: boolean;
  /** AC2 : pourquoi elle n'est pas modifiable ; null si elle l'est. */
  raison: string | null;
}

/** US-23 : réponse de GET /contrats/{numero}/garanties. */
export interface GarantiesContrat {
  numero: string;
  produit: Produit;
  formule: string;
  statut: StatutContrat;
  /** Prénom du client (jamais son nom). */
  prenom: string | null;
  cotisationMensuelle: number;
  cotisationAnnuelle: number;
  frequencePaiement: string | null;
  garanties: GarantieProposee[];
  /** AC5 : l'avenant préparé et pas encore accepté. */
  avenantEnAttente: ProjetAvenant | null;
}

/** US-23, AC4 : avenant accepté. */
export interface AvenantAccepte {
  avenant: Avenant;
  /** Faux si le PDF est encore en préparation. */
  disponible: boolean;
  emailEnvoye: boolean;
  /** AC7 : nouvelle version des conditions particulières. */
  versionConditionsParticulieres: number | null;
}

export const LIBELLES_STATUT_AVENANT: Record<StatutAvenant, string> = {
  EN_ATTENTE: 'En attente',
  ACCEPTE: 'Accepté',
  ABANDONNE: 'Abandonné',
};

/** Montant au format « 18,50 € TTC » (espace insécable avant €). */
export function montantTtc(montant: number): string {
  return `${montant.toFixed(2).replace('.', ',')} € TTC`;
}

/** Date ISO (« 2026-11-27 ») au format JJ/MM/AAAA. */
export function dateJjMmAaaa(date: string | null): string {
  return date ? date.split('-').reverse().join('/') : '';
}

/** Horodatage au format « 26/11/2026 à 15:05 » (heure de Paris). */
export function dateHeureFrancaise(instant: string | null): string {
  if (!instant) return '';
  const moment = new Date(instant);
  const jour = moment.toLocaleDateString('fr-FR', { timeZone: 'Europe/Paris' });
  const heure = moment.toLocaleTimeString('fr-FR', { timeZone: 'Europe/Paris', hour: '2-digit', minute: '2-digit' });
  return `${jour} à ${heure}`;
}

/** Codes des garanties souscrites aujourd'hui. */
export function garantiesSouscrites(garanties: GarantieProposee[]): string[] {
  return garanties.filter((garantie) => garantie.souscrite).map((garantie) => garantie.code);
}

/** « Continuer » : vrai si au moins une garantie a changé par rapport au contrat (sélection = codes voulus). */
export function aChange(garanties: GarantieProposee[], selection: ReadonlySet<string>): boolean {
  return garanties.some((garantie) => selection.has(garantie.code) !== garantie.souscrite);
}

/** Nouvelle cotisation mensuelle affichée en direct : actuelle + ajouts − retraits (même calcul que le serveur). */
export function cotisationApres(
  cotisationMensuelle: number,
  garanties: GarantieProposee[],
  selection: ReadonlySet<string>,
): number {
  const delta = garanties.reduce((somme, garantie) => {
    const voulue = selection.has(garantie.code);
    if (voulue === garantie.souscrite || garantie.prixMensuelTtc === null) return somme;
    return somme + (voulue ? garantie.prixMensuelTtc : -garantie.prixMensuelTtc);
  }, 0);
  return Math.round((cotisationMensuelle + delta) * 100) / 100;
}

/**
 * RG3 : quand s'applique la nouvelle cotisation. Mensuel : première mensualité après la date d'effet, sans prorata ;
 * annuel : prochaine échéance annuelle.
 */
export function messageApplication(projet: ProjetAvenant): string {
  const echeance = projet.premiereEcheanceConcernee ? ` (${dateJjMmAaaa(projet.premiereEcheanceConcernee)})` : '';
  if (projet.frequencePaiement === 'ANNUEL') {
    return `Vous payez à l'année : la nouvelle cotisation s'appliquera à votre prochaine échéance annuelle${echeance}.`;
  }
  return `La nouvelle mensualité s'appliquera à partir de votre première échéance après la date d'effet${echeance}, sans prorata.`;
}
