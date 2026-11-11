import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContratResume } from '../../core/model/contrat';
import { MesContrats } from './mes-contrats';

describe('MesContrats (US-04, AC1)', () => {
  let fixture: ComponentFixture<MesContrats>;
  let page: HTMLElement;
  let http: HttpTestingController;

  const contrat: ContratResume = {
    referenceDevis: 'ref-1',
    numero: 'AE-2026-000001',
    statut: 'ACTIF',
    adresseLogement: '12 rue des Lilas',
    codePostal: '75001',
    ville: 'Paris',
    cotisationMensuelle: 18.5,
    dateEffet: '2026-12-01',
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

  it('affiche le chargement puis mes contrats', () => {
    expect(page.textContent).toContain('Chargement de vos contrats');
    repondre([contrat]);

    expect(page.querySelector('h2')?.textContent).toContain('AE-2026-000001');
    expect(page.querySelector('.badge')?.textContent).toBe('Actif');
    expect(page.querySelector('.devis-ligne a')?.getAttribute('href')).toBe('/espace-client/contrats/ref-1');
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
