import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { DossierClient } from './dossier-client';

describe('DossierClient (US-04, AC4)', () => {
  let fixture: ComponentFixture<DossierClient>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DossierClient);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const repondre = (corps: object, status = 200): void => {
    http.expectOne('/api/assurance-iard-api/back-office/clients/1').flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };

  const contrat = {
    referenceDevis: 'r',
    numero: 'AE-2026-000001',
    statut: 'ACTIF',
    adresseLogement: '12 rue des Lilas',
    codePostal: '75001',
    ville: 'Paris',
    cotisationMensuelle: 18.5,
    dateEffet: '2026-12-01',
    titulaire: 'Alice Martin',
    ibanFin: '0189',
    jourPrelevement: 10,
    rum: 'AE-RUM-1',
  };

  it("affiche les informations de paiement avec l'IBAN masqué : seuls les 4 derniers caractères", () => {
    expect(page.textContent).toContain('Chargement du dossier');
    repondre({
      id: 1,
      civilite: 'madame',
      prenom: 'Alice',
      nom: 'Martin',
      email: 'a@exemple.fr',
      telephone: '0612345678',
      profil: 'ASSURE',
      contrats: [contrat, { ...contrat, numero: 'AE-2026-000002', titulaire: null, ibanFin: null }],
      documents: [{ id: 5, categorie: 'domicile', nomFichier: 'facture.pdf' }],
    });

    expect(page.querySelector('h1')?.textContent).toBe('Alice Martin');
    expect(Array.from(page.querySelectorAll('.iban')).map((i) => i.textContent)).toEqual(['•••• •••• •••• 0189']);
    expect(page.textContent).toContain('Justificatif de domicile : facture.pdf');
  });

  it('dit quand le client n’a ni contrat ni document', () => {
    repondre({ id: 1, prenom: null, nom: null, email: 'p@exemple.fr', telephone: null, profil: 'PROSPECT', contrats: [], documents: [] });

    expect(page.textContent).toContain('Aucun contrat.');
    expect(page.textContent).toContain('Aucun document.');
    expect(page.textContent).toContain('Profil : Prospect');
  });

  it('affiche un message clair si le dossier ne peut pas être chargé', () => {
    repondre({}, 404);
    expect(page.querySelector('[role="alert"]')?.textContent).toContain("n'a pas pu être chargé");
  });
});
