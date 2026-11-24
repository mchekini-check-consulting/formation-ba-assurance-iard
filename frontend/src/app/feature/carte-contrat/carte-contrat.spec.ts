import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContratResume } from '../../core/model/contrat';
import { CONTRAT_AUTO } from '../../core/model/contrat.exemples';
import { BadgeStatut } from '../badge-statut/badge-statut';
import { CarteContrat } from './carte-contrat';

describe('CarteContrat (US-18, AC1, AC2, AC5)', () => {
  let fixture: ComponentFixture<CarteContrat>;
  let carte: HTMLElement;

  const afficher = (contrat: ContratResume): void => {
    fixture.componentRef.setInput('contrat', contrat);
    fixture.detectChanges();
    carte = fixture.nativeElement as HTMLElement;
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    fixture = TestBed.createComponent(CarteContrat);
  });

  it('affiche numéro, produit, bien assuré, prime, échéance et statut en texte', () => {
    afficher(CONTRAT_AUTO);

    expect(carte.querySelector('h3')?.textContent).toContain('Auto · AE-RECETTE-A03');
    expect(carte.textContent).toContain('Renault Clio · AB-003-CD');
    expect(carte.textContent).toContain('45,00');
    expect(carte.textContent).toContain('Échéance : 25/09/2027');
    expect(carte.querySelector('.badge-statut')?.textContent?.trim()).toBe('Actif');
    expect(carte.textContent).toContain('Votre contrat est actif.');
  });

  it('montre seulement les raccourcis livrés : « Voir le détail » vers la fiche par numéro', () => {
    afficher(CONTRAT_AUTO);

    const liens = Array.from(carte.querySelectorAll('a'));
    expect(liens.map((lien) => lien.textContent?.trim())).toEqual(['Voir le détail']);
    expect(liens[0].getAttribute('href')).toBe('/espace-client/contrats/AE-RECETTE-A03');
  });

  it('propose « Signer » pour un contrat habitation en attente de signature', () => {
    afficher({
      ...CONTRAT_AUTO,
      produit: 'HABITATION',
      statut: 'EN_ATTENTE_DE_SIGNATURE',
      referenceDevis: '7c9e6679-7425-40de-944b-e07fc1f90ae7',
    });

    const signer = Array.from(carte.querySelectorAll('a')).find((lien) => lien.textContent?.includes('Signer'));
    expect(signer?.getAttribute('href')).toBe('/devis/contrat?reference=7c9e6679-7425-40de-944b-e07fc1f90ae7');
  });

  it('affiche la date de fin à la place de l’échéance pour un contrat qui se termine', () => {
    afficher({ ...CONTRAT_AUTO, statut: 'RESILIE', dateEcheance: null, dateFin: '2026-11-19', bienAssure: null });

    expect(carte.textContent).toContain('Fin du contrat : 19/11/2026');
    expect(carte.querySelector('.badge-statut--gris')?.textContent?.trim()).toBe('Résilié');
  });
});

describe('BadgeStatut (US-18, AC5)', () => {
  it('écrit toujours le nom du statut, avec sa couleur en accompagnement', () => {
    TestBed.configureTestingModule({});
    const fixture = TestBed.createComponent(BadgeStatut);
    fixture.componentRef.setInput('statut', 'IMPAYE');
    fixture.detectChanges();
    const badge = (fixture.nativeElement as HTMLElement).querySelector('span');

    expect(badge?.textContent).toBe('Impayé');
    expect(badge?.className).toContain('badge-statut--rouge');
  });
});
