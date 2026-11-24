import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CONTRAT_AUTO, CONTRAT_HABITATION } from '../../core/model/contrat.exemples';
import { MesContrats } from './mes-contrats';

describe('MesContrats (US-04, AC1 / US-18)', () => {
  let fixture: ComponentFixture<MesContrats>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MesContrats);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const repondre = (corps: object, status = 200): void => {
    http.expectOne('/api/assurance-iard-api/contrats').flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };

  it('affiche le chargement puis mes contrats habitation et auto', () => {
    expect(page.textContent).toContain('Chargement de vos contrats');
    repondre([CONTRAT_HABITATION, CONTRAT_AUTO]);

    const titres = Array.from(page.querySelectorAll('h3')).map((titre) => titre.textContent?.trim());
    expect(titres).toEqual(['Appartement · Créteil', 'Renault Clio · AB-003-CD']);
    expect(page.querySelector('.badge-statut')?.textContent?.trim()).toBe('Actif');
    expect(page.querySelector('header')).toBeNull();
  });

  it('ouvre la fiche par numéro, et non plus par référence de devis', () => {
    repondre([CONTRAT_HABITATION]);
    expect(page.querySelector('.carte a')?.getAttribute('href')).toBe('/espace-client/contrats/AE-2026-000001');
  });

  it("dit quand il n'y a aucun contrat", () => {
    repondre([]);
    expect(page.textContent).toContain("Vous n'avez pas encore de contrat.");
  });

  it('affiche un message clair si le chargement échoue', () => {
    repondre({}, 500);
    expect(page.querySelector('[role="alert"]')?.textContent).toContain("n'ont pas pu être chargés");
  });
});
