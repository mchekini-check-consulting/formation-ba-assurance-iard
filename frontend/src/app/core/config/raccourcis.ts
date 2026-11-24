import { ContratResume, Produit } from '../model/contrat';
import { ActionContrat, decrireStatut } from '../model/statut-contrat';

/** Ce dont les raccourcis ont besoin d'un contrat (résumé ou fiche). */
export type ContratRaccourcis = Pick<ContratResume, 'numero' | 'produit' | 'statut' | 'referenceDevis'>;

/**
 * US-18, AC2 : réglage des raccourcis. Une fonction pas encore livrée reste masquée.
 * À la livraison d'une US : passer `livre` à true et renseigner `lien` (adresse de l'écran).
 *   modifierGaranties → US-23 ; regenerer → US-21 ; genererFacture → US-22 ; declarerSinistre → US-24.
 * `produits` : produits pour lesquels la fonction existe (signature : habitation seulement, US-33 pas livrée).
 */
export const RACCOURCIS: Record<
  ActionContrat,
  { libelle: string; livre: boolean; produits: readonly Produit[]; lien?: (contrat: ContratRaccourcis) => Lien | null }
> = {
  detail: {
    libelle: 'Voir le détail',
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
  modifierGaranties: { libelle: 'Modifier mes garanties', livre: false, produits: ['HABITATION', 'AUTO'] },
  regenerer: { libelle: 'Régénérer mon contrat', livre: false, produits: ['HABITATION', 'AUTO'] },
  genererFacture: { libelle: 'Générer une facture', livre: false, produits: ['HABITATION', 'AUTO'] },
  declarerSinistre: { libelle: 'Déclarer un sinistre', livre: false, produits: ['HABITATION', 'AUTO'] },
};

export interface Lien {
  commandes: string[];
  parametres?: Record<string, string>;
}

export interface Raccourci {
  action: ActionContrat;
  libelle: string;
  lien: Lien;
}

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
