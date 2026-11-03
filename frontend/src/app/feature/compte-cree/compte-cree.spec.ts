import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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
    expect(page.textContent).toContain('Vous êtes connecté.');
    expect(page.querySelector('strong')?.textContent).toBe('marie@exemple.fr');
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
});
