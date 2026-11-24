import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContratResume } from '../../core/model/contrat';
import { CONTRAT_AUTO } from '../../core/model/contrat.exemples';
import { MesContrats } from './mes-contrats';

describe('MesContrats (US-04, AC1 / US-18)', () => {
  let fixture: ComponentFixture<MesContrats>;
  let page: HTMLElement;
  let http: HttpTestingController;

  const habitation: ContratResume = {
    referenceDevis: 'ref-1',
    numero: 'AE-2026-000001',
    produit: 'HABITATION',
    statut: 'ACTIF',
    bienAssure: '12 rue des Lilas, 75001 Paris',
    cotisationMensuelle: 18.5,
    dateEffet: '2026-12-01',
    dateEcheance: '2027-12-01',
    dateFin: null,
  };

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
    repondre([habitation, CONTRAT_AUTO]);

    const titres = Array.from(page.querySelectorAll('h3')).map((titre) => titre.textContent?.trim());
    expect(titres).toEqual(['Habitation · AE-2026-000001', 'Auto · AE-RECETTE-A03']);
    expect(page.querySelector('.badge-statut')?.textContent?.trim()).toBe('Actif');
  });

  it('ouvre la fiche par numéro, et non plus par référence de devis', () => {
    repondre([habitation]);
    expect(page.querySelector('.devis-ligne a')?.getAttribute('href')).toBe('/espace-client/contrats/AE-2026-000001');
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
