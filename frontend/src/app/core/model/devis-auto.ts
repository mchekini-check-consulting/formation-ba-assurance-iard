/** US-27 : données d'un devis auto, stockées côté client pendant les étapes 1.1 à 1.4. */

export interface VehiculeAuto {
  refVehiculeId: number;
  immatriculation: string | null;
  marque: string;
  modele: string;
  energie: string;
  version: string | null;
  annee: number | null;
  valeurNeuve: number | null;
}

export type ModeAchat = 'COMPTANT' | 'CREDIT' | 'LOA' | 'LLD';
export type TitulaireCarteGrise = 'SOUSCRIPTEUR' | 'CONJOINT' | 'ENFANT' | 'SOCIETE_LOCATION' | 'AUTRE_PERSONNE';
export type UsageAuto = 'PRIVE' | 'TRAJET_TRAVAIL' | 'PROFESSIONNEL';
export type StationnementAuto = 'GARAGE_FERME' | 'PARKING_PRIVE' | 'RUE';
export type KmAnnuels = 'MOINS_8000' | '8000_15000' | '15001_25000' | 'PLUS_25000';
export type ResiliationAuto = 'AUCUNE' | 'NON_PAIEMENT' | 'SINISTRALITE' | 'FAUSSE_DECLARATION' | 'AUTRE_MOTIF';
export type ConducteurPrincipal = 'MOI_MEME' | 'ENFANT' | 'CONJOINT' | 'AUTRE_PERSONNE_FOYER';
export type AssureVehicule = 'NON' | 'OUI_MOINS_UN_AN' | 'OUI_PLUS_UN_AN';
export type SinistresCount = 0 | 1 | 2 | 3;

export interface AchatAuto {
  immatriculation: string | null;
  // TODO étape 4 (Mes informations) : si plaqueRenseignee === false, demander la plaque
  // (obligatoire). Si nonImmatricule === true, demander le VIN à la place.
  plaqueRenseignee: boolean;
  nonImmatricule: boolean;
  dateAcquisition: string;
  modeAchat: ModeAchat;
  titulaireCarteGrise: TitulaireCarteGrise;
}

export interface UsageAutoSaisie {
  usage: UsageAuto;
  stationnement: StationnementAuto;
  kmAnnuels: KmAnnuels;
  codePostal: string;
}

export interface ConducteurAuto {
  conducteurPrincipal: ConducteurPrincipal;
  dateNaissance: string;
  datePermis: string;
  bonusMalus: number;
  anneesAssurance: number;
  conduiteAccompagnee: boolean;
  nbSinistresResponsables: SinistresCount;
  nbSinistresNonResponsables: SinistresCount;
  resiliationAssureur: ResiliationAuto;
  suspensionAnnulation: boolean;
  condamnationAlcool: boolean;
  assureVehicule: AssureVehicule;
  ancienAssureur: string;
  ancienNumeroContrat: string;
}

export function kmAnnuelsVersEntier(km: KmAnnuels): number {
  switch (km) {
    case 'MOINS_8000': return 7000;
    case '8000_15000': return 11500;
    case '15001_25000': return 20000;
    case 'PLUS_25000': return 30000;
  }
}

export interface SivRechercheResponse {
  statut: 'TROUVE' | 'INCONNU' | 'ERREUR' | 'DELAI';
  immatriculation: string;
  refVehiculeId?: number;
  marque?: string;
  modele?: string;
  energie?: string;
  version?: string;
  annee?: number;
  valeurNeuve?: number;
}

export interface ModeleVehicule {
  id: number;
  modele: string;
  energie: string;
  version: string | null;
  annee: number | null;
  valeurNeuve: number | null;
}

export interface MarqueVehicule {
  marque: string;
  modeles: ModeleVehicule[];
}

export interface CriteresEligibilite {
  bonusMalusMaxLigne: number;
  resiliationBloquante: string;
}
