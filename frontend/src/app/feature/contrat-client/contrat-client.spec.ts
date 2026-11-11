import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { ContratClient } from './contrat-client';

describe('ContratClient (US-04)', () => {
  let fixture: ComponentFixture<ContratClient>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ reference: 'ref-1' }) } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ContratClient);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const repondre = (corps: object, status = 200): void => {
    http.expectOne('/api/assurance-iard-api/devis/contrat/ref-1').flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };

  it('affiche le contrat avec un IBAN masqué (RG4)', () => {
    expect(page.textContent).toContain('Chargement du contrat');
    repondre({
      numero: 'AE-2026-000001',
      statut: 'ACTIF',
      adresseLogement: '12 rue des Lilas',
      codePostal: '75001',
      ville: 'Paris',
      cotisationMensuelle: 18.5,
      cotisationAnnuelle: 222,
      dateEffet: '2026-12-01',
      jourPrelevement: 10,
      ibanFin: '0189',
      rum: 'AE-RUM-1',
    });

    expect(page.querySelector('h1')?.textContent).toContain('AE-2026-000001');
    expect(page.querySelector('.iban')?.textContent).toBe('•••• •••• •••• 0189');
  });

  it('dit si le contrat est introuvable', () => {
    repondre({}, 404);
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('introuvable');
  });

  it("affiche un message clair si le contrat n'a pas pu être chargé", () => {
    repondre({}, 500);
    expect(page.querySelector('[role="alert"]')?.textContent).toContain("n'a pas pu être chargé");
  });
});
