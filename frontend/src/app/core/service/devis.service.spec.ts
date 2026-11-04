import { TestBed } from '@angular/core/testing';
import { DevisService } from './devis.service';

describe('DevisService', () => {
  let service: DevisService;

  beforeEach(() => {
    service = TestBed.inject(DevisService);
  });

  it('reprend le tarif de l’étape 1', () => {
    expect(service.tarifBase()).toBeNull();
    service.mettreAJour({ surface: '60', typeLogement: 'appartement' });
    expect(service.tarifBase()).toBe(9.4);
  });

  it('met la recommandation à jour à chaque réponse', () => {
    expect(service.recommandation()).toBeNull();
    service.mettreAJourBesoins({ valeurContenu: 25000 });
    expect(service.recommandation()?.formule).toBe('serenite');
  });

  it('sélectionne la formule conseillée une fois la recommandation enregistrée', () => {
    service.validerRecommandation('ref-1', 'confort');
    expect(service.reference()).toBe('ref-1');
    expect(service.formuleChoisie()).toBe('confort');
  });

  it('oublie la référence dès qu’une réponse change', () => {
    service.validerRecommandation('ref-1', 'confort');
    service.mettreAJourBesoins({ litiges: true });
    expect(service.reference()).toBeNull();
  });
});
