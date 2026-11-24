import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContratResume } from '../../core/model/contrat';
import { CONTRAT_AUTO, CONTRAT_HABITATION } from '../../core/model/contrat.exemples';
import { BadgeStatut } from '../badge-statut/badge-statut';
import { CarteContrat } from './carte-contrat';

describe('CarteContrat (US-18, maquette ; AC1, AC2, AC5)', () => {
  let fixture: ComponentFixture<CarteContrat>;
  let carte: HTMLElement;

  const afficher = (contrat: ContratResume): void => {
    fixture.componentRef.setInput('contrat', contrat);
    fixture.detectChanges();
    carte = fixture.nativeElement as HTMLElement;
  };
  const texte = (selecteur: string): string =>
    (carte.querySelector(selecteur)?.textContent ?? '').replace(/\s+/g, ' ').trim();

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    fixture = TestBed.createComponent(CarteContrat);
  });

  it('habitation : produit et statut en haut, « Appartement · Créteil » en titre, formule, numéro et prime', () => {
    afficher(CONTRAT_HABITATION);

    expect(texte('.carte__produit')).toBe('Habitation');
    expect(texte('.badge-statut')).toBe('Actif');
    expect(texte('h3')).toBe('Appartement · Créteil');
    expect(texte('.carte__detail')).toBe('Formule Confort · N° AE-2026-000001');
    expect(texte('.carte__prime')).toBe('14,90 €/mois TTC');
    expect(texte('.carte__message')).toBe('Votre contrat est actif.');
  });

  it('auto : le véhicule en titre', () => {
    afficher(CONTRAT_AUTO);

    expect(texte('.carte__produit')).toBe('Auto');
    expect(texte('h3')).toBe('Renault Clio · AB-003-CD');
    expect(texte('.carte__detail')).toBe('Formule Tous risques · N° AE-RECETTE-A03');
  });

  it('sans type de logement ni ville, le bien assuré sert de titre', () => {
    afficher({ ...CONTRAT_HABITATION, typeLogement: null, ville: null, formule: null });

    expect(texte('h3')).toBe('12 rue des Lilas, 94000 Créteil');
    expect(texte('.carte__detail')).toBe('N° AE-2026-000001');
  });

  it('« Voir mon contrat » (plein) ouvre la fiche par numéro ; les fonctions non livrées sont masquées', () => {
    afficher(CONTRAT_AUTO);

    const liens = Array.from(carte.querySelectorAll('a'));
    expect(liens.map((lien) => lien.textContent?.trim())).toEqual(['Voir mon contrat']);
    expect(liens[0].getAttribute('href')).toBe('/espace-client/contrats/AE-RECETTE-A03');
    expect(liens[0].classList).not.toContain('btn--contour');
  });

  it('« Signer » (contour) pour un contrat habitation en attente de signature', () => {
    afficher({ ...CONTRAT_HABITATION, statut: 'EN_ATTENTE_DE_SIGNATURE' });

    const signer = Array.from(carte.querySelectorAll('a')).find((lien) => lien.textContent?.includes('Signer'));
    expect(signer?.getAttribute('href')).toBe('/devis/contrat?reference=7c9e6679-7425-40de-944b-e07fc1f90ae7');
    expect(signer?.classList).toContain('btn--contour');
  });

  it('contrat résilié : badge gris avec le texte du statut', () => {
    afficher({ ...CONTRAT_AUTO, statut: 'RESILIE', dateEcheance: null, dateFin: '2026-11-19', bienAssure: null });

    expect(texte('.badge-statut--gris')).toBe('Résilié');
    expect(texte('h3')).toBe('Contrat AE-RECETTE-A03');
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
