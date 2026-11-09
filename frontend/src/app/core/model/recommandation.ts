
export type Formule = 'essentielle' | 'confort' | 'serenite';
export type Remboursement = 'neuf' | 'vetuste';
export type Priorite = 'prix' | 'equilibre' | 'protection';
export type OptionConseillee = 'objets-valeur' | 'assistance-depannage' | 'protection-juridique';
export type Franchise = '150' | '300' | '500';
export type Periodicite = 'mensuel' | 'annuel';

export interface ReponsesBesoins {
  valeurContenu: number | null;
  objetsValeur: boolean | null;
  appareilsCouteux: boolean | null;
  remboursement: Remboursement | null;
  assistance: boolean | null;
  litiges: boolean | null;
  priorite: Priorite | null;
}

export interface Recommandation {
  formule: Formule;
  raisons: string[];
  options: OptionConseillee[];
}

export interface DetailFormule {
  nom: string;
  description: string;
  plafond: string;
  plafondValeur: number;
  garanties: string[];
}

export const PLAFOND_SERENITE = 35_000;

export const FORMULES: Record<Formule, DetailFormule> = {
  essentielle: {
    nom: 'Essentielle',
    description: 'Les garanties indispensables : dégâts des eaux, incendie, vol et responsabilité civile.',
    plafond: '10 000 €',
    plafondValeur: 10_000,
    garanties: ['Dégâts des eaux', 'Incendie & explosion', 'Vol & vandalisme', 'Responsabilité civile'],
  },
  confort: {
    nom: 'Confort',
    description: "L'équilibre entre protection et budget, pour la plupart des logements.",
    plafond: '20 000 €',
    plafondValeur: 20_000,
    garanties: [
      'Dégâts des eaux',
      'Incendie & explosion',
      'Vol & vandalisme',
      'Responsabilité civile',
      'Bris de glace',
      'Catastrophes naturelles',
      'Tempête, grêle, neige',
    ],
  },
  serenite: {
    nom: 'Sérénité',
    description: 'La protection la plus large, avec remboursement à neuf.',
    plafond: '35 000 €',
    plafondValeur: 35_000,
    garanties: [
      'Dégâts des eaux',
      'Incendie & explosion',
      'Vol & vandalisme',
      'Responsabilité civile',
      'Bris de glace',
      'Catastrophes naturelles',
      'Tempête, grêle, neige',
      'Remboursement à neuf',
      'Dommages électriques',
    ],
  },
};

export const LIBELLES_OPTIONS: Record<OptionConseillee, string> = {
  'objets-valeur': 'Objets de valeur',
  'assistance-depannage': 'Assistance dépannage',
  'protection-juridique': 'Protection juridique',
};

// PROVISOIRE : rang pour comparer les formules (alerte choix inférieur à la recommandation)
export const RANG_FORMULE: Record<Formule, number> = { essentielle: 0, confort: 1, serenite: 2 };

export function reponsesVides(): ReponsesBesoins {
  return {
    valeurContenu: null,
    objetsValeur: null,
    appareilsCouteux: null,
    remboursement: null,
    assistance: null,
    litiges: null,
    priorite: null,
  };
}

export function reponsesCompletes(reponses: ReponsesBesoins): boolean {
  return Object.values(reponses).every((reponse) => reponse !== null);
}

export function depassePlafondSerenite(reponses: ReponsesBesoins): boolean {
  return reponses.valeurContenu !== null && reponses.valeurContenu > PLAFOND_SERENITE;
}

export function formaterPrix(prix: number): string {
  return prix.toFixed(2).replace('.', ',');
}
