import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ResultatCreationCompte } from '../model/compte';
import { CompteService } from './compte.service';

describe('CompteService', () => {
  let service: CompteService;
  let http: HttpTestingController;
  let resultat: ResultatCreationCompte | undefined;

  const creer = (email: string): void => {
    service.creerCompte({ email, motDePasse: 'Abcdefg1' }).subscribe((r) => (resultat = r));
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CompteService);
    http = TestBed.inject(HttpTestingController);
    resultat = undefined;
  });

  afterEach(() => http.verify());

  it("n'a personne de connecté au départ", () => {
    expect(service.emailConnecte()).toBeNull();
  });

  it('envoie POST /comptes au backend, puis connecte la personne', () => {
    creer(' Marie@Exemple.fr ');
    const requete = http.expectOne('/api/comptes');
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toEqual({ email: 'Marie@Exemple.fr', motDePasse: 'Abcdefg1' });
    requete.flush({ id: 7, email: 'marie@exemple.fr' }, { status: 201, statusText: 'Created' });

    expect(resultat).toBe('cree');
    expect(service.emailConnecte()).toBe('marie@exemple.fr');
  });

  it('signale un e-mail déjà utilisé quand le backend répond 409 (RG1)', () => {
    creer('deja@exemple.fr');
    http.expectOne('/api/comptes').flush({ detail: 'Un compte existe déjà avec cette adresse.' }, { status: 409, statusText: 'Conflict' });

    expect(resultat).toBe('email-deja-utilise');
    expect(service.emailConnecte()).toBeNull();
  });

  it('signale une erreur technique si le backend ne répond pas', () => {
    creer('marie@exemple.fr');
    http.expectOne('/api/comptes').error(new ProgressEvent('error'), { status: 0 });

    expect(resultat).toBe('erreur-technique');
    expect(service.emailConnecte()).toBeNull();
  });
});
