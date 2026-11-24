import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DocumentsContratService, ResultatTelechargement, enregistrerFichier } from './documents-contrat.service';

describe('DocumentsContratService (US-18, AC3)', () => {
  let service: DocumentsContratService;
  let http: HttpTestingController;
  let resultats: ResultatTelechargement[];

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DocumentsContratService);
    http = TestBed.inject(HttpTestingController);
    resultats = [];
  });

  afterEach(() => http.verify());

  it('télécharge le PDF par la route de T-03', () => {
    service.telecharger(12).subscribe((r) => resultats.push(r));
    const requete = http.expectOne('/api/assurance-iard-api/documents-contrat/12');
    expect(requete.request.responseType).toBe('blob');
    const pdf = new Blob(['%PDF'], { type: 'application/pdf' });
    requete.flush(pdf);
    expect(resultats).toEqual([{ statut: 'telecharge', contenu: pdf }]);
  });

  it('409 : le document est en préparation', () => {
    service.telecharger(12).subscribe((r) => resultats.push(r));
    http
      .expectOne('/api/assurance-iard-api/documents-contrat/12')
      .flush(new Blob(), { status: 409, statusText: 'x' });
    expect(resultats).toEqual([{ statut: 'en-preparation' }]);
  });

  it('une autre erreur est signalée sans détail', () => {
    service.telecharger(12).subscribe((r) => resultats.push(r));
    http
      .expectOne('/api/assurance-iard-api/documents-contrat/12')
      .flush(new Blob(), { status: 500, statusText: 'x' });
    expect(resultats).toEqual([{ statut: 'erreur' }]);
  });

  it('propose le fichier au navigateur puis libère la mémoire', () => {
    const creer = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:adresse');
    const liberer = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    enregistrerFichier(new Blob(['%PDF']), 'cp.pdf');

    expect(creer).toHaveBeenCalled();
    expect(clic).toHaveBeenCalled();
    expect(liberer).toHaveBeenCalledWith('blob:adresse');
    vi.restoreAllMocks();
  });
});
