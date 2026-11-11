import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { GestionProfils } from './gestion-profils';

const PERMISSIONS_ASSURE = [
  { rubrique: 'DEVIS', droit: 'CONSULTER', valeur: true },
  { rubrique: 'DEVIS', droit: 'CREER', valeur: true },
  { rubrique: 'PROFILS', droit: 'CONSULTER', valeur: false },
];

const PROFILS_EXEMPLE = [
  { nom: 'PROSPECT', description: 'Prospect', nombreUtilisateurs: 2, permissions: [] },
  { nom: 'ASSURE', description: 'Assure', nombreUtilisateurs: 5, permissions: PERMISSIONS_ASSURE },
  { nom: 'GESTIONNAIRE', description: 'Gestionnaire', nombreUtilisateurs: 1, permissions: [] },
  {
    nom: 'ADMINISTRATEUR',
    description: 'Admin',
    nombreUtilisateurs: 1,
    permissions: [{ rubrique: 'PROFILS', droit: 'CONSULTER', valeur: true }],
  },
];

@Component({ template: '' })
class PageVide {}

describe('GestionProfils', () => {
  let http: HttpTestingController;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let c: any;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: '', component: PageVide }])],
    });
    http = TestBed.inject(HttpTestingController);
    c = TestBed.createComponent(GestionProfils).componentInstance;
    http.expectOne('/api/assurance-iard-api/admin/profils').flush(PROFILS_EXEMPLE);
  });

  afterEach(() => http.verify());

  it('charge les 4 profils et selectionne le premier', () => {
    expect(c.profils()?.length).toBe(4);
    expect(c.profilActif()).toBe('PROSPECT');
  });

  it('change le profil actif', () => {
    c.selectionnerProfil('ASSURE');
    expect(c.profilActif()).toBe('ASSURE');
  });

  it('lit la valeur originale une permission', () => {
    c.selectionnerProfil('ASSURE');
    expect(c.valeurPermission('DEVIS', 'CONSULTER')).toBe(true);
    expect(c.valeurPermission('PROFILS', 'CONSULTER')).toBe(false);
  });

  it('bascule une permission et detecte les modifications', () => {
    c.selectionnerProfil('ASSURE');
    expect(c.aDesModifications()).toBe(false);
    c.basculerPermission('DEVIS', 'CREER');
    expect(c.valeurPermission('DEVIS', 'CREER')).toBe(false);
    expect(c.aDesModifications()).toBe(true);
  });

  it('annule une modification en rebasculant', () => {
    c.selectionnerProfil('ASSURE');
    c.basculerPermission('DEVIS', 'CREER');
    c.basculerPermission('DEVIS', 'CREER');
    expect(c.aDesModifications()).toBe(false);
  });

  it('ne bascule pas les droits PROFILS de administrateur', () => {
    c.selectionnerProfil('ADMINISTRATEUR');
    expect(c.estVerrouille('PROFILS')).toBe(true);
    c.basculerPermission('PROFILS', 'CONSULTER');
    expect(c.aDesModifications()).toBe(false);
  });

  it('enregistre les permissions et met a jour localement', () => {
    c.selectionnerProfil('ASSURE');
    c.basculerPermission('DEVIS', 'CREER');
    c.demanderConfirmation();
    expect(c.confirmation()).toBe(true);
    c.confirmerEnregistrement();
    http
      .expectOne('/api/assurance-iard-api/admin/profils/ASSURE/permissions')
      .flush(null, { status: 204, statusText: 'No Content' });
    expect(c.confirmation()).toBe(false);
    expect(c.aDesModifications()).toBe(false);
  });

  it("affiche une erreur si l'enregistrement echoue", () => {
    c.selectionnerProfil('ASSURE');
    c.basculerPermission('DEVIS', 'CREER');
    c.confirmerEnregistrement();
    http
      .expectOne('/api/assurance-iard-api/admin/profils/ASSURE/permissions')
      .flush(null, { status: 500, statusText: 'Server Error' });
    expect(c.erreur()).toBeTruthy();
  });

  it('annule la demande de confirmation', () => {
    c.demanderConfirmation();
    c.annulerConfirmation();
    expect(c.confirmation()).toBe(false);
  });

  it("charge et affiche l'historique", () => {
    c.afficherOuMasquerHistorique();
    http.expectOne('/api/assurance-iard-api/admin/profils/PROSPECT/historique').flush([
      { modifieLe: '2026-11-10T10:00:00Z', rubrique: 'DEVIS', droit: 'CREER', ancienneValeur: true, nouvelleValeur: false },
    ]);
    expect(c.historique()?.length).toBe(1);
    expect(c.afficherHistorique()).toBe(true);
  });

  it("masque l'historique au second clic", () => {
    c.afficherOuMasquerHistorique();
    http.expectOne('/api/assurance-iard-api/admin/profils/PROSPECT/historique').flush([]);
    c.afficherOuMasquerHistorique();
    expect(c.afficherHistorique()).toBe(false);
  });

  it('formate les dates en francais', () => {
    const date = c.formaterDate('2026-11-10T10:30:00Z');
    expect(date).toContain('10');
    expect(date).toContain('11');
    expect(date).toContain('2026');
  });
});
