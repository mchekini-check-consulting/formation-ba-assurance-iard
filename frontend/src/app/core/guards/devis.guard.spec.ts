import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { DevisService } from '../service/devis.service';
import { logementRenseigneGuard, recommandationEnregistreeGuard } from './devis.guard';

describe('gardes du devis', () => {
  let devis: DevisService;
  const route = {} as ActivatedRouteSnapshot;
  const etat = {} as RouterStateSnapshot;
  const adresse = (resultat: unknown): string => TestBed.inject(Router).serializeUrl(resultat as UrlTree);

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    devis = TestBed.inject(DevisService);
  });

  it('étape 2 : renvoie vers l’étape 1 sans tarif', () => {
    const resultat = TestBed.runInInjectionContext(() => logementRenseigneGuard(route, etat));
    expect(adresse(resultat)).toBe('/devis/logement');
  });

  it('étape 2 : laisse passer avec un tarif', () => {
    devis.mettreAJour({ surface: '60', typeLogement: 'maison' });
    expect(TestBed.runInInjectionContext(() => logementRenseigneGuard(route, etat))).toBe(true);
  });

  it('choix de formule : renvoie vers l’étape 2 sans recommandation enregistrée', () => {
    const resultat = TestBed.runInInjectionContext(() => recommandationEnregistreeGuard(route, etat));
    expect(adresse(resultat)).toBe('/devis/besoins');
  });

  it('choix de formule : laisse passer une fois la recommandation enregistrée', () => {
    devis.validerRecommandation('ref-1', 'confort');
    expect(TestBed.runInInjectionContext(() => recommandationEnregistreeGuard(route, etat))).toBe(true);
  });
});
