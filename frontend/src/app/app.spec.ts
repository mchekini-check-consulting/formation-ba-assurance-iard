import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { CompteService } from './core/service/compte.service';

describe('App (routes)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
  });

  const titre = (harness: RouterTestingHarness): string | undefined =>
    harness.routeNativeElement?.querySelector('h1')?.textContent ?? undefined;

  it("affiche la landing page à l'accueil", async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(titre(harness)).toContain('Votre logement assuré en ligne');
  });

  it.each([
    ['/cookies', 'Cookies'],
    ['/cgu', 'CGU'],
    ['/accessibilite', 'Accessibilité'],
    ['/contact', 'Contact'],
  ])('affiche la page %s avec son titre', async (adresse, titre) => {
    const harness = await RouterTestingHarness.create(adresse);
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe(titre);
  });

  it("renvoie une adresse inconnue vers l'accueil", async () => {
    const harness = await RouterTestingHarness.create('/page-inconnue');
    expect(titre(harness)).toContain('Votre logement assuré en ligne');
  });

  it('affiche la création de compte sur /inscription', async () => {
    const harness = await RouterTestingHarness.create('/inscription');
    expect(titre(harness)).toContain('Créer mon compte');
  });

  it("renvoie vers /inscription si personne n'est connecté", async () => {
    const harness = await RouterTestingHarness.create('/compte-cree');
    expect(titre(harness)).toContain('Créer mon compte');
  });

  it('affiche « Compte créé » une fois le compte créé', async () => {
    TestBed.inject(CompteService).emailConnecte.set('marie@exemple.fr');
    const harness = await RouterTestingHarness.create('/compte-cree');
    expect(titre(harness)).toContain('Compte créé');
  });
});
