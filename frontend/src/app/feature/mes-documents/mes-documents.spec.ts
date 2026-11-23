import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MesDocuments } from './mes-documents';

describe('MesDocuments (US-04, AC1)', () => {
  let fixture: ComponentFixture<MesDocuments>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MesDocuments);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const repondre = (corps: object, status = 200): void => {
    http.expectOne('/api/assurance-iard-api/documents').flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };

  it('liste mes documents avec un lien pour les ouvrir', () => {
    expect(page.textContent).toContain('Chargement de vos documents');
    repondre([{ id: 5, referenceDevis: 'r', categorie: 'identite', typeDocument: 'passeport', nomFichier: 'p.pdf', typeFichier: 'pdf', taille: 3 }]);

    expect(page.querySelector('h2')?.textContent).toBe("Pièce d'identité");
    expect(page.querySelector('.devis-ligne a')?.getAttribute('href')).toBe('/espace-client/documents/5');
  });

  it("dit quand il n'y a aucun document", () => {
    repondre([]);
    expect(page.textContent).toContain("Vous n'avez pas encore déposé de document.");
  });

  it('affiche un message clair si le chargement échoue', () => {
    repondre({}, 500);
    expect(page.querySelector('[role="alert"]')).not.toBeNull();
  });
});
