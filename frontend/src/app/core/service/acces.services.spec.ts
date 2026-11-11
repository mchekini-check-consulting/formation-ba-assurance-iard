import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ibanMasque, ibanMasqueDepuisFin } from '../model/iban';
import { nomComplet } from '../model/back-office';
import { libelleCategorie } from '../model/document';
import { BackOfficeService } from './back-office.service';
import { ContratService } from './contrat.service';
import { DocumentsService } from './documents.service';

/** US-04 : appels des nouvelles routes (« Mes contrats », « Mes documents », back-office, journal). */
describe('services US-04', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const resultatDe = <T>(appel: { subscribe: (f: (v: T) => void) => unknown }): (() => T | undefined) => {
    let valeur: T | undefined;
    appel.subscribe((v) => (valeur = v));
    return () => valeur;
  };

  it('liste mes contrats', () => {
    const resultat = resultatDe(TestBed.inject(ContratService).lister());
    http.expectOne('/api/assurance-iard-api/contrats').flush([{ numero: 'AE-1' }]);
    expect(resultat()).toEqual([{ numero: 'AE-1' }]);
  });

  it('liste et ouvre mes documents', () => {
    const service = TestBed.inject(DocumentsService);
    const liste = resultatDe(service.lister());
    http.expectOne('/api/assurance-iard-api/documents').flush([{ id: 5 }]);
    expect(liste()).toEqual([{ id: 5 }]);

    const contenu = resultatDe(service.ouvrir('5'));
    const requete = http.expectOne('/api/assurance-iard-api/documents/5');
    expect(requete.request.responseType).toBe('blob');
    requete.flush(new Blob(['pdf']));
    expect(contenu()).toBeInstanceOf(Blob);
  });

  it('interroge le back-office et le journal', () => {
    const service = TestBed.inject(BackOfficeService);
    const clients = resultatDe(service.listerClients());
    http.expectOne('/api/assurance-iard-api/back-office/clients').flush([]);
    const dossier = resultatDe(service.dossier('3'));
    http.expectOne('/api/assurance-iard-api/back-office/clients/3').flush({ id: 3 });
    const journal = resultatDe(service.accesRefuses());
    http.expectOne('/api/assurance-iard-api/admin/acces-refuses').flush([]);

    expect(clients()).toEqual([]);
    expect(dossier()).toEqual({ id: 3 });
    expect(journal()).toEqual([]);
  });

  it('renvoie null quand le chargement échoue', () => {
    const contrats = resultatDe(TestBed.inject(ContratService).lister());
    http.expectOne('/api/assurance-iard-api/contrats').flush({}, { status: 500, statusText: 'Erreur' });
    const documents = resultatDe(TestBed.inject(DocumentsService).ouvrir('9'));
    http.expectOne('/api/assurance-iard-api/documents/9').flush(new Blob(), { status: 403, statusText: 'Interdit' });
    const clients = resultatDe(TestBed.inject(BackOfficeService).listerClients());
    http.expectOne('/api/assurance-iard-api/back-office/clients').flush({}, { status: 403, statusText: 'Interdit' });

    expect(contrats()).toBeNull();
    expect(documents()).toBeNull();
    expect(clients()).toBeNull();
  });

  it('RG4 : un seul masquage d’IBAN, 4 derniers caractères visibles', () => {
    expect(ibanMasqueDepuisFin('0189')).toBe('•••• •••• •••• 0189');
    expect(ibanMasqueDepuisFin(null)).toBe('•••• •••• •••• ••••');
    expect(ibanMasque('FR76 3000 6000 0112 3456 7890 189')).toBe('•••• •••• •••• 0189');
  });

  it('libellés', () => {
    expect(nomComplet({ prenom: 'Alice', nom: 'Martin', email: 'a@exemple.fr' })).toBe('Alice Martin');
    expect(nomComplet({ prenom: null, nom: null, email: 'a@exemple.fr' })).toBe('a@exemple.fr');
    expect(libelleCategorie('identite')).toBe("Pièce d'identité");
    expect(libelleCategorie('autre')).toBe('autre');
  });
});
