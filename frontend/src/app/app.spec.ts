import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';

describe('App (routes)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
  });

  it("affiche la landing page à l'accueil", async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Votre logement assuré en ligne');
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
    expect(harness.routeNativeElement?.querySelector('h1')).not.toBeNull();
  });
});
