import {
  ReponsesBesoins,
  depassePlafondSerenite,
  formaterPrix,
  prixFormule,
  recommander,
  reponsesCompletes,
  reponsesVides,
} from './recommandation';

/** Situation sans aucune condition de RG1, qui mène à Essentielle si rien n'est changé. */
const essentielle: ReponsesBesoins = {
  valeurContenu: 5000,
  objetsValeur: false,
  appareilsCouteux: false,
  remboursement: 'vetuste',
  assistance: false,
  litiges: false,
  priorite: 'prix',
};

describe('recommander', () => {
  describe('RG1 : Sérénité', () => {
    it('contenu supérieur à 20 000 €', () => {
      const r = recommander({ ...essentielle, valeurContenu: 25000 });
      expect(r?.formule).toBe('serenite');
      expect(r?.raisons).toEqual(['Votre contenu dépasse 20 000 €']);
    });

    it('remboursement à neuf souhaité', () => {
      const r = recommander({ ...essentielle, remboursement: 'neuf' });
      expect(r?.formule).toBe('serenite');
      expect(r?.raisons).toEqual(['Vous souhaitez un remboursement à neuf']);
    });

    it('appareils coûteux', () => {
      const r = recommander({ ...essentielle, appareilsCouteux: true });
      expect(r?.formule).toBe('serenite');
      expect(r?.raisons).toEqual(['Vous possédez des appareils coûteux']);
    });

    it('priorité à la protection la plus large', () => {
      const r = recommander({ ...essentielle, priorite: 'protection' });
      expect(r?.formule).toBe('serenite');
      expect(r?.raisons).toEqual(['Vous privilégiez la protection la plus large']);
    });

    it('cite toutes les raisons quand plusieurs conditions sont remplies', () => {
      const r = recommander({ ...essentielle, valeurContenu: 30000, remboursement: 'neuf' });
      expect(r?.raisons).toHaveLength(2);
    });
  });

  describe('RG2 : Essentielle', () => {
    it('contenu inférieur à 10 000 €, aucun objet de valeur et priorité au prix', () => {
      const r = recommander(essentielle);
      expect(r?.formule).toBe('essentielle');
      expect(r?.raisons).toEqual([
        'Votre contenu est inférieur à 10 000 €',
        'Vous ne déclarez aucun objet de valeur',
        'Votre priorité est le prix',
      ]);
    });

    it.each([
      ['des objets de valeur', { objetsValeur: true }, 'Vous déclarez des objets de valeur'],
      ['une autre priorité', { priorite: 'equilibre' }, "Vous recherchez l'équilibre entre protection et budget"],
      ['un contenu plus élevé', { valeurContenu: 15000 }, 'Votre contenu est compris entre 10 000 € et 20 000 €'],
    ] as [string, Partial<ReponsesBesoins>, string][])('Confort avec %s', (_cas, modification, raison) => {
      const r = recommander({ ...essentielle, ...modification });
      expect(r?.formule).toBe('confort');
      expect(r?.raisons).toEqual([raison]);
    });
  });

  it('conflit RG1/RG2 : RG1 est prioritaire', () => {
    expect(recommander({ ...essentielle, appareilsCouteux: true })?.formule).toBe('serenite');
  });

  describe('seuils', () => {
    it.each([
      [9999, 'essentielle'],
      [10000, 'confort'],
      [10001, 'confort'],
      [19999, 'confort'],
      [20000, 'confort'],
      [20001, 'serenite'],
    ])('contenu de %i € : %s', (valeurContenu, formule) => {
      expect(recommander({ ...essentielle, valeurContenu })?.formule).toBe(formule);
    });
  });

  describe('RG3 : options conseillées', () => {
    it('aucune option sans demande', () => {
      expect(recommander(essentielle)?.options).toEqual([]);
    });

    it('objets de valeur, si déclarés', () => {
      expect(recommander({ ...essentielle, objetsValeur: true })?.options).toEqual(['objets-valeur']);
    });

    it('assistance dépannage, si souhaitée', () => {
      expect(recommander({ ...essentielle, assistance: true })?.options).toEqual(['assistance-depannage']);
    });

    it('protection juridique, si accompagnement en cas de litige souhaité', () => {
      expect(recommander({ ...essentielle, litiges: true })?.options).toEqual(['protection-juridique']);
    });

    it('cumule les trois options', () => {
      expect(
        recommander({ ...essentielle, objetsValeur: true, assistance: true, litiges: true })?.options,
      ).toEqual(['objets-valeur', 'assistance-depannage', 'protection-juridique']);
    });
  });

  describe('AC2 : réponses partielles', () => {
    it('aucune formule sans réponse', () => {
      expect(recommander(reponsesVides())).toBeNull();
    });

    it('Sérénité dès la première condition remplie', () => {
      expect(recommander({ ...reponsesVides(), valeurContenu: 25000 })?.formule).toBe('serenite');
    });

    it.each(['valeurContenu', 'remboursement', 'appareilsCouteux', 'priorite', 'objetsValeur'])(
      'aucune formule tant que « %s » peut encore la changer',
      (cle) => {
        expect(recommander({ ...essentielle, [cle]: null })).toBeNull();
      },
    );

    it('Confort sans attendre les objets de valeur si Essentielle est déjà exclue', () => {
      expect(recommander({ ...essentielle, valeurContenu: 15000, objetsValeur: null })?.formule).toBe('confort');
    });

    it('assistance et litiges ne changent jamais la formule', () => {
      expect(recommander({ ...essentielle, assistance: null, litiges: null })?.formule).toBe('essentielle');
    });
  });
});

describe('reponsesCompletes', () => {
  it('exige les 7 réponses', () => {
    expect(reponsesCompletes(reponsesVides())).toBe(false);
    expect(reponsesCompletes({ ...essentielle, litiges: null })).toBe(false);
    expect(reponsesCompletes(essentielle)).toBe(true);
  });
});

describe('depassePlafondSerenite', () => {
  it.each([
    [null, false],
    [35000, false],
    [35001, true],
  ])('contenu de %s € : %s', (valeurContenu, attendu) => {
    expect(depassePlafondSerenite({ ...essentielle, valeurContenu })).toBe(attendu);
  });
});

describe('prix', () => {
  it.each([
    ['essentielle', 10],
    ['confort', 13],
    ['serenite', 16],
  ] as const)('%s : prix avant options', (formule, attendu) => {
    expect(prixFormule(10, formule)).toBe(attendu);
  });

  it('arrondit au centime et utilise la virgule', () => {
    expect(prixFormule(9.4, 'confort')).toBe(12.22);
    expect(formaterPrix(12.2)).toBe('12,20');
  });
});
