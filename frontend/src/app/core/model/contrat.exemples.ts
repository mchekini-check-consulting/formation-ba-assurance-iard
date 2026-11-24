import { ContratResume, FicheContrat } from './contrat';

// Exemples partagés par les tests de l'espace client (US-18), d'après les données de recette T-05.

export const CONTRAT_AUTO: ContratResume = {
  referenceDevis: null,
  numero: 'AE-RECETTE-A03',
  produit: 'AUTO',
  statut: 'ACTIF',
  bienAssure: 'Renault Clio · AB-003-CD',
  cotisationMensuelle: 45,
  dateEffet: '2026-09-25',
  dateEcheance: '2027-09-25',
  dateFin: null,
};

export const FICHE_AUTO: FicheContrat = {
  numero: 'AE-RECETTE-A03',
  referenceDevis: null,
  produit: 'AUTO',
  statut: 'ACTIF',
  formule: 'TR',
  logement: null,
  vehicule: { marque: 'Renault', modele: 'Clio', version: 'V TCe 90', immatriculation: 'AB-003-CD' },
  franchise: 500,
  garanties: [{ code: 'RC', prixMensuelTtc: 12 }],
  cotisationMensuelle: 45,
  cotisationAnnuelle: 540,
  dateEffet: '2026-09-25',
  dateEcheance: '2027-09-25',
  dateFin: null,
  paiement: { frequence: 'MENSUEL', jourPrelevement: 5, ibanMasque: '•••• 7788', rum: 'AE-RUM-RECETTE-A' },
  documents: [
    { id: 12, type: 'CONDITIONS_PARTICULIERES', libelle: 'Conditions particulières', version: 1, dateCreation: null },
    { id: 13, type: 'CONDITIONS_GENERALES', libelle: 'Conditions générales', version: 1, dateCreation: null },
  ],
};
