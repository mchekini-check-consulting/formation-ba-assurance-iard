import { Formule, Franchise, OptionConseillee, Recommandation } from './recommandation';
import { Tarif } from './tarif';

const PRIX: Record<Formule, number> = { essentielle: 9.4, confort: 12.22, serenite: 15.04 };
const OPTIONS: Record<OptionConseillee, number> = {
  'objets-valeur': 2.5,
  'assistance-depannage': 1.5,
  'protection-juridique': 2.9,
};
const FRANCHISES: Record<Franchise, number> = { '150': 1.5, '300': 0, '500': -1 };

export const RECO_CONFORT: Recommandation = {
  formule: 'confort',
  raisons: ['Vous déclarez des objets de valeur'],
  options: ['objets-valeur', 'assistance-depannage'],
};

export function tarifEstime(): Tarif {
  const ordre: Formule[] = ['essentielle', 'confort', 'serenite'];
  return {
    idTarif: 'tarif-1',
    tarifable: true,
    prix: ordre.map((formule) => ({
      formule,
      mensuel: PRIX[formule],
      annuel: Math.round(PRIX[formule] * 1200) / 100,
    })),
    prixOptions: (Object.keys(OPTIONS) as OptionConseillee[]).map((option) => ({ option, prixMensuel: OPTIONS[option] })),
    ajustementsFranchise: (Object.keys(FRANCHISES) as Franchise[]).map((franchise) => ({
      franchise: Number(franchise),
      ajustement: FRANCHISES[franchise],
    })),
  };
}

export function tarifDetaille(
  formule: Formule = 'confort',
  options: OptionConseillee[] = [],
  franchise: Franchise | null = null,
): Tarif {
  const ajustement = franchise ? FRANCHISES[franchise] : 0;
  const total = PRIX[formule] + options.reduce((somme, option) => somme + OPTIONS[option], 0) + ajustement;
  const totalMensuel = Math.round(total * 100) / 100;
  return {
    ...tarifEstime(),
    formuleConseillee: RECO_CONFORT.formule,
    raisons: RECO_CONFORT.raisons,
    optionsConseillees: RECO_CONFORT.options,
    detail: {
      formule,
      prixFormule: PRIX[formule],
      options: options.map((option) => ({ option, prixMensuel: OPTIONS[option] })),
      ajustementFranchise: ajustement,
      totalMensuel,
      totalAnnuel: Math.round(totalMensuel * 1200) / 100,
    },
  };
}
