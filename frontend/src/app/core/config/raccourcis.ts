import { ContratResume, Produit } from '../model/contrat';
import { ActionContrat, decrireStatut } from '../model/statut-contrat';

/**
 * US-18 : LE réglage des fonctions de l'espace client. Une fonction pas encore livrée est masquée partout : raccourcis
 * des contrats (AC2), navigation et tuiles « Que voulez-vous faire ? ».
 * À la livraison d'une US : passer sa ligne à true, puis renseigner son adresse ci-dessous (ADRESSES) et, pour un
 * raccourci de contrat, son `lien` dans RACCOURCIS.
 */
export const LIVRAISONS = {
  /** US-23 : livrée (Modifier mes garanties, Relire et accepter l'avenant, depuis la fiche du contrat) */
  modifierGaranties: true,
  /** US-21 : livrée (régénération depuis la section « Versions » de la fiche contrat) */
  regenerer: true,
  /** US-22 : livrée (Mes factures, Générer une facture) */
  factures: true,
  /** US-24 : livrée (Mes sinistres, dans la barre latérale) */
  sinistres: true,
  /** US-24 : livrée (Déclarer un sinistre : tuile, raccourci des contrats ouverts à la déclaration) */
  declarerSinistre: true,
  /** Page « Mon compte » (pas encore développée) */
  monCompte: false,
};

/** Adresse des pages des fonctions livrées (à renseigner à la livraison). */
export const ADRESSES: Partial<Record<keyof typeof LIVRAISONS, string>> = {
  /** US-21 : la régénération se fait sur la fiche du contrat (section « Versions »), sous cette adresse. */
  regenerer: '/espace-client/contrats',
  /** US-22 : échéancier et factures ; « Générer une facture » d'un contrat ouvre la page sur ce contrat. */
  factures: '/espace-client/factures',
  /**
   * US-23 : la tuile « Modifier mes garanties » ouvre « Mes contrats » (choix du contrat) ; le raccourci d'un contrat
   * ouvre directement <adresse>/<numéro>/garanties.
   */
  modifierGaranties: '/espace-client/contrats',
  /** US-24 : dossiers et brouillons. */
  sinistres: '/espace-client/sinistres',
  /** US-24 : étape 1 de la déclaration ; le raccourci d'un contrat ajoute ?contrat=<numéro>. */
  declarerSinistre: '/espace-client/sinistres/declarer',
};

/** US-24, AC1 : un contrat résilié reste ouvert à la déclaration 2 ans après sa date de fin. */
export const ANNEES_DECLARATION_APRES_RESILIATION = 2;

/** US-24, AC1 (décision BA) : même règle que le serveur (ReglesDeclaration.declarable). */
export function resilieDepuisMoinsDe2Ans(dateFin: string | null | undefined, aujourdhui = new Date()): boolean {
  if (!dateFin) return false;
  const limite = new Date(aujourdhui);
  limite.setFullYear(limite.getFullYear() - ANNEES_DECLARATION_APRES_RESILIATION);
  return new Date(`${dateFin}T00:00:00`) > limite;
}

/** US-23 : écrans de l'avenant, sous la fiche du contrat. */
export const CHEMIN_GARANTIES = 'garanties';
export const CHEMIN_AVENANT = 'avenant';
export const CHEMIN_AVENANT_ACCEPTE = 'avenant-accepte';

/** US-21 : ancre de la section « Versions » de la fiche contrat. */
export const ANCRE_VERSIONS = 'versions';

/** Ce dont les raccourcis ont besoin d'un contrat (résumé ou fiche). */
export type ContratRaccourcis = Pick<ContratResume, 'numero' | 'produit' | 'statut' | 'referenceDevis'> &
  Partial<Pick<ContratResume, 'dateFin'>>;

export interface Lien {
  commandes: string[];
  parametres?: Record<string, string>;
  /** Section de la page à afficher (ancre). */
  fragment?: string;
}

export interface Raccourci {
  action: ActionContrat;
  libelle: string;
  lien: Lien;
}

/**
 * AC2 : raccourcis d'un contrat. `produits` : produits pour lesquels la fonction existe (signature : habitation
 * seulement, US-33 pas livrée).
 */
export const RACCOURCIS: Record<
  ActionContrat,
  { libelle: string; livre: boolean; produits: readonly Produit[]; lien?: (contrat: ContratRaccourcis) => Lien | null }
> = {
  detail: {
    libelle: 'Voir mon contrat',
    livre: true,
    produits: ['HABITATION', 'AUTO'],
    lien: (contrat) => ({ commandes: ['/espace-client/contrats', contrat.numero] }),
  },
  signer: {
    libelle: 'Signer mon contrat',
    livre: true,
    produits: ['HABITATION'],
    lien: (contrat) =>
      contrat.referenceDevis
        ? { commandes: ['/devis/contrat'], parametres: { reference: contrat.referenceDevis } }
        : null,
  },
  modifierGaranties: {
    libelle: 'Modifier mes garanties',
    livre: LIVRAISONS.modifierGaranties,
    produits: ['HABITATION', 'AUTO'],
    // US-23 : écran « Modifier mes garanties » du contrat (AC6 : proposé seulement si le statut le permet).
    lien: (contrat) =>
      ADRESSES.modifierGaranties
        ? { commandes: [ADRESSES.modifierGaranties, contrat.numero, CHEMIN_GARANTIES] }
        : null,
  },
  relireAvenant: {
    libelle: 'Relire et accepter mon avenant',
    livre: LIVRAISONS.modifierGaranties,
    produits: ['HABITATION', 'AUTO'],
    // US-23, AC5 : écran « Relire et accepter l'avenant » du contrat.
    lien: (contrat) =>
      ADRESSES.modifierGaranties ? { commandes: [ADRESSES.modifierGaranties, contrat.numero, CHEMIN_AVENANT] } : null,
  },
  regenerer: {
    libelle: 'Régénérer mon contrat',
    livre: LIVRAISONS.regenerer,
    produits: ['HABITATION', 'AUTO'],
    // US-21 : ouvre la fiche du contrat sur sa section « Versions », où se trouve le bouton de régénération.
    lien: (contrat) =>
      ADRESSES.regenerer
        ? { commandes: [ADRESSES.regenerer, contrat.numero], fragment: ANCRE_VERSIONS }
        : null,
  },
  genererFacture: {
    libelle: 'Générer une facture',
    livre: LIVRAISONS.factures,
    produits: ['HABITATION', 'AUTO'],
    // US-22 : ouvre « Mes factures » sur ce contrat (ancre contrat-<numéro>), où se trouvent les boutons.
    lien: (contrat) =>
      ADRESSES.factures ? { commandes: [ADRESSES.factures], fragment: `contrat-${contrat.numero}` } : null,
  },
  declarerSinistre: {
    libelle: 'Déclarer un sinistre',
    livre: LIVRAISONS.declarerSinistre,
    produits: ['HABITATION', 'AUTO'],
    // US-24, AC1 : ouvre l'étape 1 sur ce contrat (le client l'a choisi) ; un contrat résilié depuis 2 ans ou plus
    // n'a pas ce raccourci.
    lien: (contrat) => {
      if (!ADRESSES.declarerSinistre) return null;
      if (contrat.statut === 'RESILIE' && !resilieDepuisMoinsDe2Ans(contrat.dateFin)) return null;
      return { commandes: [ADRESSES.declarerSinistre], parametres: { contrat: contrat.numero } };
    },
  },
};

/** Raccourcis affichés : fonction livrée, permise par le statut (RG3) et par le produit, avec une adresse. */
export function raccourcisDuContrat(
  contrat: ContratRaccourcis,
  reglage: typeof RACCOURCIS = RACCOURCIS,
): Raccourci[] {
  return decrireStatut(contrat.statut).actions.flatMap((action) => {
    const raccourci = reglage[action];
    if (!raccourci.livre || !raccourci.produits.includes(contrat.produit) || !raccourci.lien) {
      return [];
    }
    const lien = raccourci.lien(contrat);
    return lien ? [{ action, libelle: raccourci.libelle, lien }] : [];
  });
}

/** Une entrée de navigation ou une tuile : visible si la fonction est livrée et a une adresse. */
export interface Entree {
  libelle: string;
  adresse: string;
  /** Page d'accueil de l'espace : active seulement sur son adresse exacte. */
  exacte?: boolean;
}

function siLivree(fonction: keyof typeof LIVRAISONS, libelle: string, livraisons = LIVRAISONS, adresses = ADRESSES): Entree[] {
  const adresse = adresses[fonction];
  return livraisons[fonction] && adresse ? [{ libelle, adresse }] : [];
}

/**
 * Navigation client, dans l'ordre de la maquette. Les droits du profil (US-04) sont vérifiés par l'écran :
 * `rubrique` indique le droit de consultation nécessaire.
 */
export function navigationClient(
  livraisons = LIVRAISONS,
  adresses = ADRESSES,
): (Entree & { rubrique?: 'CONTRATS' | 'DEVIS' | 'DOCUMENTS' | 'SINISTRES' })[] {
  return [
    { libelle: 'Tableau de bord', adresse: '/espace-client', exacte: true },
    { libelle: 'Mes contrats', adresse: '/espace-client/contrats', rubrique: 'CONTRATS' as const },
    { libelle: 'Mes devis', adresse: '/espace-client/devis', rubrique: 'DEVIS' as const },
    // US-04 : pièces déposées à la souscription (ajout à la maquette, validé par le BA).
    { libelle: 'Mes documents', adresse: '/espace-client/documents', rubrique: 'DOCUMENTS' as const },
    ...siLivree('factures', 'Mes factures', livraisons, adresses).map((e) => ({ ...e, rubrique: 'CONTRATS' as const })),
    ...siLivree('sinistres', 'Mes sinistres', livraisons, adresses).map((e) => ({ ...e, rubrique: 'SINISTRES' as const })),
    ...siLivree('monCompte', 'Mon compte', livraisons, adresses),
  ];
}

/** « Que voulez-vous faire ? » : tuiles des fonctions livrées ; « Faire un nouveau devis » est toujours disponible. */
export function tuiles(livraisons = LIVRAISONS, adresses = ADRESSES): Entree[] {
  return [
    ...siLivree('declarerSinistre', 'Déclarer un sinistre', livraisons, adresses),
    ...siLivree('modifierGaranties', 'Modifier mes garanties', livraisons, adresses),
    ...siLivree('factures', 'Mes factures', livraisons, adresses),
    // Page d'accueil : elle propose le devis habitation et le devis auto (US-26).
    { libelle: 'Faire un nouveau devis', adresse: '/' },
  ];
}

/** Lien « Voir mes factures » / « Voir mes sinistres » des cartes du tableau de bord ; null tant que non livré. */
export function adresseSiLivree(fonction: keyof typeof LIVRAISONS, livraisons = LIVRAISONS, adresses = ADRESSES): string | null {
  return siLivree(fonction, '', livraisons, adresses)[0]?.adresse ?? null;
}

/**
 * Liens du back-office (US-04), dans le bloc « Espace interne » de la barre latérale, pour les profils internes
 * seulement. `rubrique` : droit de consultation nécessaire ; `administrateur` : réservé à l'administrateur.
 */
export function navigationInterne(): (Entree & {
  rubrique?: 'COMPTES_UTILISATEURS' | 'PROFILS';
  administrateur?: boolean;
})[] {
  return [
    { libelle: 'Dossiers clients', adresse: '/back-office/clients', rubrique: 'COMPTES_UTILISATEURS' },
    { libelle: 'Gérer les profils', adresse: '/admin/profils', administrateur: true },
    { libelle: 'Journal des accès refusés', adresse: '/admin/acces-refuses', rubrique: 'PROFILS' },
  ];
}
