import { DevisLogement } from './devis';
import { Formule, Franchise, OptionConseillee, Recommandation, ReponsesBesoins } from './recommandation';

export interface PrixFormule {
  formule: Formule;
  mensuel: number;
  annuel: number;
}

export interface PrixOption {
  option: OptionConseillee;
  prixMensuel: number;
}

export interface AjustementFranchise {
  franchise: number;
  ajustement: number;
}

export interface DetailTarif {
  formule: Formule;
  prixFormule: number;
  options: PrixOption[];
  ajustementFranchise: number;
  totalMensuel: number;
  totalAnnuel: number;
}

export interface Tarif {
  idTarif: string;
  tarifable: boolean;
  motif?: string;
  prix?: PrixFormule[];
  detail?: DetailTarif;
  formuleConseillee?: Formule;
  raisons?: string[];
  optionsConseillees?: OptionConseillee[];
  prixOptions?: PrixOption[];
  ajustementsFranchise?: AjustementFranchise[];
}

export interface DemandeTarif {
  logement: DevisLogement;
  besoins?: ReponsesBesoins;
  formule?: Formule | null;
  options?: OptionConseillee[];
  franchise?: Franchise | null;
}

export function recommandationDe(tarif: Tarif): Recommandation | null {
  if (!tarif.formuleConseillee) return null;
  return { formule: tarif.formuleConseillee, raisons: tarif.raisons ?? [], options: tarif.optionsConseillees ?? [] };
}
