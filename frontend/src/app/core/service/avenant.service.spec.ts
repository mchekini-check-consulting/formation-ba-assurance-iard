import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AVENANT_A03, GARANTIES_A03, PROJET_A03 } from '../model/avenant.exemples';
import {
  AvenantService,
  MESSAGE_AUCUN_AVENANT,
  MESSAGE_ERREUR_TECHNIQUE,
  MESSAGE_NON_MODIFIABLE,
  MESSAGE_TARIFS_INDISPONIBLES,
  ResultatAcceptation,
  ResultatAbandon,
  ResultatGaranties,
  ResultatPreparation,
} from './avenant.service';

/** En local, apiBase renvoie /api/assurance-iard-api (relais du serveur de dev, comme le gateway). */
const BASE = '/api/assurance-iard-api/contrats/AE-RECETTE-A03';

describe('AvenantService (US-23)', () => {
  let service: AvenantService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AvenantService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('AC1 : lit les garanties du contrat', () => {
    let resultat: ResultatGaranties | undefined;
    service.garanties('AE-RECETTE-A03').subscribe((r) => (resultat = r));
    const requete = http.expectOne(`${BASE}/garanties`);
    expect(requete.request.method).toBe('GET');
    requete.flush(GARANTIES_A03);
    expect(resultat).toEqual({ statut: 'obtenu', garanties: GARANTIES_A03 });
  });

  it('AC6 : un contrat qui n’est pas actif est refusé avec un message clair', () => {
    let resultat: ResultatGaranties | undefined;
    service.garanties('AE-RECETTE-A03').subscribe((r) => (resultat = r));
    http.expectOne(`${BASE}/garanties`).flush({}, { status: 422, statusText: 'Unprocessable' });
    expect(resultat).toEqual({ statut: 'non-modifiable', message: MESSAGE_NON_MODIFIABLE });
  });

  it('AC3 : prépare l’avenant avec les garanties voulues', () => {
    let resultat: ResultatPreparation | undefined;
    service.preparer('AE-RECETTE-A03', ['RC', 'ASSISTANCE_0KM']).subscribe((r) => (resultat = r));
    const requete = http.expectOne(`${BASE}/avenants`);
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toEqual({ garanties: ['RC', 'ASSISTANCE_0KM'] });
    requete.flush(PROJET_A03);
    expect(resultat).toEqual({ statut: 'prepare', projet: PROJET_A03 });
  });

  it('AC2 : le message du serveur (garantie obligatoire) est affiché tel quel', () => {
    let resultat: ResultatPreparation | undefined;
    service.preparer('AE-RECETTE-A03', ['PROTECTION_JURIDIQUE']).subscribe((r) => (resultat = r));
    http.expectOne(`${BASE}/avenants`).flush(
      { detail: 'La responsabilité civile est obligatoire.' },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(resultat).toEqual({ statut: 'invalide', message: 'La responsabilité civile est obligatoire.' });
  });

  it('AC4 : accepte l’avenant (case cochée)', () => {
    let resultat: ResultatAcceptation | undefined;
    service.accepter('AE-RECETTE-A03').subscribe((r) => (resultat = r));
    const requete = http.expectOne(`${BASE}/avenants/en-attente/acceptation`);
    expect(requete.request.body).toEqual({ accepte: true });
    const acceptation = { avenant: { ...AVENANT_A03, statut: 'ACCEPTE' }, disponible: true, emailEnvoye: true, versionConditionsParticulieres: 2 };
    requete.flush(acceptation);
    expect(resultat).toEqual({ statut: 'accepte', acceptation });
  });

  it('AC5 : abandonne l’avenant ; sans avenant en attente, un message clair', () => {
    let resultat: ResultatAbandon | undefined;
    service.abandonner('AE-RECETTE-A03').subscribe((r) => (resultat = r));
    http.expectOne(`${BASE}/avenants/en-attente/abandon`).flush({ ...AVENANT_A03, statut: 'ABANDONNE' });
    expect(resultat?.statut).toBe('abandonne');

    service.abandonner('AE-RECETTE-A03').subscribe((r) => (resultat = r));
    http.expectOne(`${BASE}/avenants/en-attente/abandon`).flush({}, { status: 404, statusText: 'Not Found' });
    expect(resultat).toEqual({ statut: 'introuvable', message: MESSAGE_AUCUN_AVENANT });
  });

  it('recette : « La demande contient des champs invalides. » (refus du moteur) n’est jamais affiché', () => {
    let resultat: ResultatGaranties | undefined;
    service.garanties('AE-RECETTE-H01').subscribe((r) => (resultat = r));
    http.expectOne('/api/assurance-iard-api/contrats/AE-RECETTE-H01/garanties').flush(
      { detail: 'La demande contient des champs invalides.', erreurs: [{ champ: 'etage', message: 'obligatoire' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(resultat).toEqual({ statut: 'invalide', message: MESSAGE_ERREUR_TECHNIQUE });

    let preparation: ResultatPreparation | undefined;
    service.preparer('AE-RECETTE-H01', ['objets-valeur']).subscribe((r) => (preparation = r));
    http
      .expectOne('/api/assurance-iard-api/contrats/AE-RECETTE-H01/avenants')
      .flush({ detail: 'Invalid request content.' }, { status: 400, statusText: 'Bad Request' });
    expect(preparation).toEqual({ statut: 'invalide', message: MESSAGE_ERREUR_TECHNIQUE });
  });

  it('erreurs techniques : jamais de donnée technique à l’écran', () => {
    let resultat: ResultatGaranties | undefined;
    service.garanties('AE-RECETTE-A03').subscribe((r) => (resultat = r));
    http.expectOne(`${BASE}/garanties`).flush({ detail: 'feign.RetryableException' }, { status: 503, statusText: 'KO' });
    expect(resultat).toEqual({ statut: 'erreur-technique', message: MESSAGE_TARIFS_INDISPONIBLES });

    service.garanties('AE-RECETTE-A03').subscribe((r) => (resultat = r));
    http.expectOne(`${BASE}/garanties`).flush(null, { status: 500, statusText: 'KO' });
    expect(resultat).toEqual({ statut: 'erreur-technique', message: MESSAGE_ERREUR_TECHNIQUE });
  });

  it('adresse de l’IPID d’une formule auto', () => {
    expect(service.adresseIpid('TR')).toBe('/api/assurance-iard-api/documents-contrat/ipid/TR');
  });
});
