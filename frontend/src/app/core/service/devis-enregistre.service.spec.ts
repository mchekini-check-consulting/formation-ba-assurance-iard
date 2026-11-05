import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DemandeDevis } from '../model/devis-enregistre';
import { logementVide } from '../model/devis';
import { DevisEnregistreService } from './devis-enregistre.service';

describe('DevisEnregistreService', () => {
  let service: DevisEnregistreService;
  let http: HttpTestingController;
  const demande: DemandeDevis = {
    reference: 'ref-1',
    logement: logementVide(),
    formule: 'confort',
    options: [],
    franchise: '300',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DevisEnregistreService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('enregistre sans envoyer de prix', () => {
    let resultat: unknown;
    service.enregistrer(demande).subscribe((r) => (resultat = r));
    const requete = http.expectOne('/api/devis/enregistres');
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toEqual(demande);
    requete.flush({ dateFinValidite: '2026-12-05' });
    expect(resultat).toEqual({ statut: 'enregistre', dateFinValidite: '2026-12-05' });
  });

  it('signale une session expirée', () => {
    let resultat: unknown;
    service.enregistrer(demande).subscribe((r) => (resultat = r));
    http.expectOne('/api/devis/enregistres').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(resultat).toEqual({ statut: 'session-expiree' });
  });

  it('signale une autre erreur', () => {
    let resultat: unknown;
    service.enregistrer(demande).subscribe((r) => (resultat = r));
    http.expectOne('/api/devis/enregistres').flush(null, { status: 500, statusText: 'Erreur' });
    expect(resultat).toEqual({ statut: 'erreur' });
  });

  it('liste les devis, null en cas d’échec', () => {
    const resultats: unknown[] = [];
    service.lister().subscribe((r) => resultats.push(r));
    http.expectOne('/api/devis/enregistres').flush([]);
    service.lister().subscribe((r) => resultats.push(r));
    http.expectOne('/api/devis/enregistres').flush(null, { status: 500, statusText: 'Erreur' });
    expect(resultats).toEqual([[], null]);
  });

  it('reprend un devis, null s’il est inconnu', () => {
    const resultats: unknown[] = [];
    service.reprendre('ref-1').subscribe((r) => resultats.push(r));
    http.expectOne('/api/devis/enregistres/ref-1/reprise').flush({ reference: 'ref-1' });
    service.reprendre('ref-2').subscribe((r) => resultats.push(r));
    http.expectOne('/api/devis/enregistres/ref-2/reprise').flush(null, { status: 404, statusText: 'Inconnu' });
    expect(resultats).toEqual([{ reference: 'ref-1' }, null]);
  });

  it('supprime un devis, false en cas d’échec', () => {
    const resultats: boolean[] = [];
    service.supprimer('ref-1').subscribe((r) => resultats.push(r));
    const requete = http.expectOne('/api/devis/enregistres/ref-1');
    expect(requete.request.method).toBe('DELETE');
    requete.flush(null, { status: 204, statusText: 'No Content' });
    service.supprimer('ref-2').subscribe((r) => resultats.push(r));
    http.expectOne('/api/devis/enregistres/ref-2').flush(null, { status: 500, statusText: 'Erreur' });
    expect(resultats).toEqual([true, false]);
  });

  it('télécharge le PDF, null en cas d’échec', () => {
    const resultats: unknown[] = [];
    const fichier = new Blob(['%PDF']);
    service.telechargerPdf(demande).subscribe((r) => resultats.push(r));
    const requete = http.expectOne('/api/devis/pdf');
    expect(requete.request.responseType).toBe('blob');
    requete.flush(fichier);
    service.telechargerPdf(demande).subscribe((r) => resultats.push(r));
    http.expectOne('/api/devis/pdf').flush(new Blob(), { status: 500, statusText: 'Erreur' });
    expect(resultats).toEqual([fichier, null]);
  });
});
