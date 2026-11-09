import { recommandationDe } from './tarif';
import { RECO_CONFORT, tarifDetaille, tarifEstime } from './tarif.exemples';

describe('recommandationDe', () => {
  it('lit la formule conseillée, ses raisons et ses options', () => {
    expect(recommandationDe(tarifDetaille())).toEqual(RECO_CONFORT);
  });

  it('ne donne rien si les besoins n’ont pas été envoyés', () => {
    expect(recommandationDe(tarifEstime())).toBeNull();
  });

  it('accepte un conseil sans raison ni option', () => {
    expect(recommandationDe({ idTarif: 'x', tarifable: true, formuleConseillee: 'confort' })).toEqual({
      formule: 'confort',
      raisons: [],
      options: [],
    });
  });
});
