// US-14, AC5 : traduit les réponses du devis en blocs lisibles pour le récapitulatif.
import { DevisLogement, StatutLogement } from './devis';
import {
  OptionDocument,
  SaisieInformations,
  TYPES_JUSTIFICATIF_DOMICILE,
  TYPES_PIECE_IDENTITE,
} from './informations';
import { FORMULES, Formule, Franchise, LIBELLES_OPTIONS, OptionConseillee, ReponsesBesoins, formaterPrix } from './recommandation';

export interface LigneRecapitulatif {
  libelle: string;
  valeur: string;
}

export interface BlocRecapitulatif {
  titre: string;
  /** Étape à rouvrir ; absent pour le bloc paiement, saisi sur l'écran même. */
  etape?: string;
  lignes: LigneRecapitulatif[];
}

export interface DonneesRecapitulatif {
  logement: DevisLogement;
  besoins: ReponsesBesoins;
  formule: Formule | null;
  options: OptionConseillee[];
  franchise: Franchise | null;
  prixMensuel: number | null;
  informations: SaisieInformations;
  colocataires: string[];
}

const STATUTS: Record<StatutLogement, string> = {
  locataire: 'Locataire',
  colocation: 'Colocation',
  'proprietaire-occupant': 'Propriétaire occupant',
  'proprietaire-non-occupant': 'Propriétaire non occupant',
};

const NOMBRE_PIECES_MAX = '10';

function ouiNon(valeur: boolean | null): string {
  return valeur ? 'Oui' : 'Non';
}

function montantMensuel(prix: number | null): string {
  return prix === null ? '' : `${formaterPrix(prix)} €/mois`;
}

function libelleDocument(types: readonly OptionDocument[], valeur: string): string {
  return types.find((type) => type.valeur === valeur)?.libelle ?? '';
}

function blocLogement(l: DevisLogement): BlocRecapitulatif {
  const lignes: LigneRecapitulatif[] = [
    { libelle: 'Situation', valeur: l.statut ? STATUTS[l.statut] : '' },
    { libelle: 'Résidence', valeur: l.residence === 'secondaire' ? 'Secondaire' : 'Principale' },
    { libelle: 'Logement', valeur: l.typeLogement === 'maison' ? 'Maison' : 'Appartement' },
    { libelle: 'Adresse', valeur: `${l.adresse}, ${l.codePostal} ${l.ville}` },
    { libelle: 'Surface', valeur: `${l.surface} m²` },
    { libelle: 'Pièces', valeur: l.nombrePieces === NOMBRE_PIECES_MAX ? '10 ou plus' : l.nombrePieces },
  ];
  return { titre: 'Mon logement', etape: '/devis/logement', lignes };
}

function blocBesoins(b: ReponsesBesoins): BlocRecapitulatif {
  const priorites = { prix: 'Le prix', equilibre: "L'équilibre", protection: 'La protection la plus large' };
  return {
    titre: 'Mes besoins',
    etape: '/devis/besoins',
    lignes: [
      { libelle: 'Valeur du contenu', valeur: `${(b.valeurContenu ?? 0).toLocaleString('fr-FR')} €` },
      { libelle: 'Objets de valeur', valeur: ouiNon(b.objetsValeur) },
      { libelle: 'Appareils coûteux', valeur: ouiNon(b.appareilsCouteux) },
      { libelle: 'Remboursement', valeur: b.remboursement === 'neuf' ? 'À neuf' : 'Vétusté déduite' },
      { libelle: 'Assistance dépannage', valeur: ouiNon(b.assistance) },
      { libelle: 'Accompagnement en cas de litige', valeur: ouiNon(b.litiges) },
      { libelle: 'Priorité', valeur: b.priorite ? priorites[b.priorite] : '' },
    ],
  };
}

function blocFormule(d: DonneesRecapitulatif): BlocRecapitulatif {
  return {
    titre: 'Ma formule',
    etape: '/devis/formule',
    lignes: [
      { libelle: 'Formule', valeur: d.formule ? FORMULES[d.formule].nom : '' },
      { libelle: 'Options', valeur: d.options.map((o) => LIBELLES_OPTIONS[o]).join(', ') || 'Aucune' },
      { libelle: 'Franchise', valeur: d.franchise ? `${d.franchise} €` : '' },
      { libelle: 'Cotisation', valeur: montantMensuel(d.prixMensuel) },
    ],
  };
}

function blocIdentite(i: SaisieInformations, colocataires: string[]): BlocRecapitulatif {
  const civilite = i.civilite === 'monsieur' ? 'Monsieur' : 'Madame';
  const piece = libelleDocument(TYPES_PIECE_IDENTITE, i.pieceIdentite.type);
  const domicile = libelleDocument(TYPES_JUSTIFICATIF_DOMICILE, i.domicile.type);
  const lignes: LigneRecapitulatif[] = [
    { libelle: 'Identité', valeur: `${civilite} ${i.prenom.trim()} ${i.nom.trim()}` },
    { libelle: 'Date de naissance', valeur: i.dateNaissance },
    { libelle: 'Téléphone', valeur: i.telephone },
    { libelle: "Pièce d'identité", valeur: `${piece} : ${i.pieceIdentite.fichier?.name ?? ''}` },
    { libelle: 'Justificatif de domicile', valeur: `${domicile} : ${i.domicile.fichier?.name ?? ''}` },
  ];
  if (colocataires.length > 0) lignes.push({ libelle: 'Colocataires', valeur: colocataires.join(', ') });
  return { titre: 'Mon identité', etape: '/devis/informations', lignes };
}

export function blocsRecapitulatif(donnees: DonneesRecapitulatif): BlocRecapitulatif[] {
  return [
    blocLogement(donnees.logement),
    blocBesoins(donnees.besoins),
    blocFormule(donnees),
    blocIdentite(donnees.informations, donnees.colocataires),
  ];
}
