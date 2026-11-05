import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { CompteService } from '../../core/service/compte.service';
import { CompteCree } from './compte-cree';

describe('CompteCree', () => {
  let page: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    TestBed.inject(CompteService).emailConnecte.set('marie@exemple.fr');
    const fixture = TestBed.createComponent(CompteCree);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  it("confirme la création et rappelle l'e-mail de confirmation (RG4)", () => {
    expect(page.querySelector('h1')?.textContent).toContain('Compte créé');
    expect(page.textContent).toContain('Votre compte est créé.');
    expect(page.textContent).not.toContain('Vous êtes connecté');
    expect(page.querySelector('strong')?.textContent).toBe('marie@exemple.fr');
  });

  it("propose de se connecter, sans considérer le client comme connecté (US-06, RG1)", () => {
    const lien = Array.from(page.querySelectorAll('a')).find((a) => a.textContent?.trim() === 'Me connecter');
    expect(lien?.getAttribute('href')).toBe('/connexion');
  });

  it('« Faire mon devis » annonce que le devis sera bientôt disponible', () => {
    const bouton = Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Faire mon devis')!;
    bouton.click();
    TestBed.tick();
    expect(page.querySelector('[role="status"]')?.textContent).toContain('Le devis en ligne sera bientôt disponible.');

    page.querySelector<HTMLButtonElement>('[aria-label="Fermer le message"]')!.click();
    TestBed.tick();
    expect(page.querySelector('[role="status"]')).toBeNull();
  });

  it('US-13 : garde le chemin vers le devis en cours', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams: { retour: '/devis/informations' } } } },
      ],
    });
    TestBed.inject(CompteService).emailConnecte.set('marie@exemple.fr');
    const fixture = TestBed.createComponent(CompteCree);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    const lien = Array.from(element.querySelectorAll('a')).find((a) => a.textContent?.trim() === 'Me connecter');
    expect(lien?.getAttribute('href')).toBe('/connexion?retour=%2Fdevis%2Finformations');
  });
});
