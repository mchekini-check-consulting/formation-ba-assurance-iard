import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DemandeInformations, InformationsService, ResultatInformations } from './informations.service';

describe('InformationsService', () => {
  const url = '/api/devis/informations';
  const demande: DemandeInformations = {
    referenceDevis: 'ref-1',
    civilite: 'madame',
    nom: 'Durand',
    prenom: 'Marie',
    dateNaissance: '1990-01-31',
    telephone: '0612345678',
    typePieceIdentite: 'passeport',
    typeJustificatifDomicile: 'gaz',
    colocataires: [],
  };
  const carte = new File(['a'], 'carte.pdf');
  const facture = new File(['b'], 'facture.pdf');

  let service: InformationsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(InformationsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const enregistrer = (): ResultatInformations[] => {
    const resultats: ResultatInformations[] = [];
    service.enregistrer(demande, carte, facture).subscribe((r) => resultats.push(r));
    return resultats;
  };

  it('lit l’identité du compte', () => {
    let identite: unknown;
    service.lire().subscribe((i) => (identite = i));
    http.expectOne(url).flush({ nom: 'Durand' });
    expect(identite).toEqual({ nom: 'Durand' });
  });

  it('laisse les champs vides si la lecture échoue', () => {
    let identite: unknown = 'non lue';
    service.lire().subscribe((i) => (identite = i));
    http.expectOne(url).flush(null, { status: 500, statusText: 'Erreur' });
    expect(identite).toBeNull();
  });

  it('envoie les informations et les deux fichiers', async () => {
    const resultats = enregistrer();
    const requete = http.expectOne(url);
    expect(requete.request.method).toBe('POST');
    const corps = requete.request.body as FormData;
    expect(corps.get('pieceIdentite')).toBe(carte);
    expect(corps.get('justificatifDomicile')).toBe(facture);
    const informations = corps.get('informations') as Blob;
    expect(informations.type).toBe('application/json');
    expect(JSON.parse(await informations.text())).toEqual(demande);

    requete.flush(null, { status: 204, statusText: 'No Content' });
    expect(resultats).toEqual([{ statut: 'enregistre' }]);
  });

  it('signale une session expirée', () => {
    const resultats = enregistrer();
    http.expectOne(url).flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(resultats).toEqual([{ statut: 'session-expiree' }]);
  });

  it('transmet le champ refusé et sa raison', () => {
    const resultats = enregistrer();
    http
      .expectOne(url)
      .flush({ champ: 'pieceIdentite', detail: 'Refusé.' }, { status: 422, statusText: 'Unprocessable' });
    expect(resultats).toEqual([{ statut: 'refuse', champ: 'pieceIdentite', message: 'Refusé.' }]);
  });

  it('signale toute autre erreur comme technique', () => {
    const resultats = enregistrer();
    http.expectOne(url).flush(null, { status: 500, statusText: 'Erreur' });
    expect(resultats).toEqual([{ statut: 'erreur-technique' }]);
  });
});
