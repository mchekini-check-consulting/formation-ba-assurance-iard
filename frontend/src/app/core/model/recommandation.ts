// US-10 : règles de recommandation, séparées de l'écran pour pouvoir les tester une par une.

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
  // PROVISOIRE : coefficients à valider, en attendant le vrai tarif par formule
  coefficient: number;
}

export const SEUIL_ESSENTIELLE = 10_000;
export const SEUIL_SERENITE = 20_000;
export const PLAFOND_SERENITE = 35_000;

export const FORMULES: Record<Formule, DetailFormule> = {
  essentielle: {
    nom: 'Essentielle',
    description: 'Les garanties indispensables : dégâts des eaux, incendie, vol et responsabilité civile.',
    plafond: '10 000 €',
    plafondValeur: 10_000,
    garanties: ['Dégâts des eaux', 'Incendie & explosion', 'Vol & vandalisme', 'Responsabilité civile'],
    coefficient: 1,
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
    coefficient: 1.3,
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
    coefficient: 1.6,
  },
};

export const LIBELLES_OPTIONS: Record<OptionConseillee, string> = {
  'objets-valeur': 'Objets de valeur',
  'assistance-depannage': 'Assistance dépannage',
  'protection-juridique': 'Protection juridique',
};

// PROVISOIRE : rang pour comparer les formules (alerte choix inférieur à la recommandation)
export const RANG_FORMULE: Record<Formule, number> = { essentielle: 0, confort: 1, serenite: 2 };

// PROVISOIRE : prix des options à valider avec le backend tarification
export const PRIX_OPTIONS: Record<OptionConseillee, number> = {
  'objets-valeur': 2.5,
  'assistance-depannage': 1.5,
  'protection-juridique': 2.9,
};

// PROVISOIRE : ajustements franchise à valider
export const AJUSTEMENTS_FRANCHISE: Record<Franchise, number> = {
  '150': 1.5,
  '300': 0,
  '500': -1.0,
};

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

// RG1 : une seule condition remplie suffit, toutes les conditions remplies sont citées
function raisonsSerenite(r: ReponsesBesoins): string[] {
  const raisons: string[] = [];
  if (r.valeurContenu !== null && r.valeurContenu > SEUIL_SERENITE) raisons.push('Votre contenu dépasse 20 000 €');
  if (r.remboursement === 'neuf') raisons.push('Vous souhaitez un remboursement à neuf');
  if (r.appareilsCouteux === true) raisons.push('Vous possédez des appareils coûteux');
  if (r.priorite === 'protection') raisons.push('Vous privilégiez la protection la plus large');
  return raisons;
}

function raisonsConfort(r: ReponsesBesoins): string[] {
  const raisons: string[] = [];
  if (r.valeurContenu !== null && r.valeurContenu >= SEUIL_ESSENTIELLE) {
    raisons.push('Votre contenu est compris entre 10 000 € et 20 000 €');
  }
  if (r.objetsValeur === true) raisons.push('Vous déclarez des objets de valeur');
  if (r.priorite === 'equilibre') raisons.push("Vous recherchez l'équilibre entre protection et budget");
  return raisons;
}

const RAISONS_ESSENTIELLE = [
  'Votre contenu est inférieur à 10 000 €',
  'Vous ne déclarez aucun objet de valeur',
  'Votre priorité est le prix',
];

/** Renvoie null tant qu'une réponse manquante peut encore changer la formule (AC2). */
function choisirFormule(r: ReponsesBesoins): Pick<Recommandation, 'formule' | 'raisons'> | null {
  const raisons = raisonsSerenite(r);
  if (raisons.length > 0) return { formule: 'serenite', raisons };

  const { valeurContenu, remboursement, appareilsCouteux, priorite, objetsValeur } = r;
  if (valeurContenu === null || remboursement === null || appareilsCouteux === null || priorite === null) {
    return null;
  }

  const confort = { formule: 'confort' as const, raisons: raisonsConfort(r) };
  if (valeurContenu >= SEUIL_ESSENTIELLE || priorite !== 'prix') return confort;
  if (objetsValeur === null) return null;
  return objetsValeur ? confort : { formule: 'essentielle', raisons: RAISONS_ESSENTIELLE };
}

// RG3
function optionsConseillees(r: ReponsesBesoins): OptionConseillee[] {
  const options: OptionConseillee[] = [];
  if (r.objetsValeur === true) options.push('objets-valeur');
  if (r.assistance === true) options.push('assistance-depannage');
  if (r.litiges === true) options.push('protection-juridique');
  return options;
}

export function recommander(reponses: ReponsesBesoins): Recommandation | null {
  const choix = choisirFormule(reponses);
  return choix && { ...choix, options: optionsConseillees(reponses) };
}

export function depassePlafondSerenite(reponses: ReponsesBesoins): boolean {
  return reponses.valeurContenu !== null && reponses.valeurContenu > PLAFOND_SERENITE;
}

/** Prix avant options : le tarif de l'étape 1 correspond à la formule Essentielle. */
export function prixFormule(tarifBase: number, formule: Formule): number {
  return Math.round(tarifBase * FORMULES[formule].coefficient * 100) / 100;
}

export function formaterPrix(prix: number): string {
  return prix.toFixed(2).replace('.', ',');
}
