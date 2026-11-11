import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ResultatCode, ResultatConnexion, ResultatRenvoi } from '../model/connexion';
import { ConnexionService } from './connexion.service';

describe('ConnexionService', () => {
  let service: ConnexionService;
  let http: HttpTestingController;

  const repondre = (url: string, corps: object | null, status: number): void => {
    http.expectOne(url).flush(corps, { status, statusText: String(status) });
  };

  const connecter = (): (() => ResultatConnexion | undefined) => {
    let resultat: ResultatConnexion | undefined;
    service.connecter(' marie@exemple.fr ', 'Abcdefg1').subscribe((r) => (resultat = r));
    return () => resultat;
  };

  const verifier = (): (() => ResultatCode | undefined) => {
    let resultat: ResultatCode | undefined;
    service.verifierCode('123456').subscribe((r) => (resultat = r));
    return () => resultat;
  };

  const renvoyer = (): (() => ResultatRenvoi | undefined) => {
    let resultat: ResultatRenvoi | undefined;
    service.renvoyerCode().subscribe((r) => (resultat = r));
    return () => resultat;
  };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ConnexionService);
    http = TestBed.inject(HttpTestingController);
    service.verification.set({ verification: 'v1', emailMasque: 'm***@exemple.fr' });
  });

  afterEach(() => http.verify());

  it('connecte : garde la vérification et son adresse masquée (AC1)', () => {
    service.verification.set(null);
    const resultat = connecter();
    const requete = http.expectOne('/api/assurance-iard-api/connexion');
    expect(requete.request.body).toEqual({ email: 'marie@exemple.fr', motDePasse: 'Abcdefg1' });
    requete.flush({ verification: 'v2', emailMasque: 'm***@exemple.fr' });

    expect(resultat()).toEqual({ statut: 'code-envoye' });
    expect(service.verification()).toEqual({ verification: 'v2', emailMasque: 'm***@exemple.fr' });
  });

  it.each([
    [401, {}, { statut: 'identifiants-incorrects' }],
    [423, { bloqueJusquA: '2026-11-04T10:15:00Z' }, { statut: 'compte-bloque', bloqueJusquA: '2026-11-04T10:15:00Z' }],
    [500, {}, { statut: 'erreur-technique' }],
  ])('traduit le refus de connexion %i', (status, corps, attendu) => {
    const resultat = connecter();
    repondre('/api/assurance-iard-api/connexion', corps, status);
    expect(resultat()).toEqual(attendu);
  });

  it('ouvre la session quand le code est bon (AC2)', () => {
    const resultat = verifier();
    const requete = http.expectOne('/api/assurance-iard-api/connexion/verification');
    expect(requete.request.body).toEqual({ verification: 'v1', code: '123456' });
    requete.flush({ jeton: 'session', email: 'marie@exemple.fr', profil: 'ASSURE' });

    expect(resultat()).toEqual({ statut: 'connecte' });
    expect(service.jeton()).toBe('session');
    expect(service.emailClient()).toBe('marie@exemple.fr');
    expect(service.profilClient()).toBe('ASSURE');
    expect(service.verification()).toBeNull();
  });

  it.each([
    [401, { essaisRestants: 2 }, { statut: 'code-incorrect', essaisRestants: 2 }],
    [409, { motif: 'essais-epuises' }, { statut: 'nouveau-code-requis', motif: 'essais-epuises' }],
    [404, {}, { statut: 'verification-perdue' }],
    [500, {}, { statut: 'erreur-technique' }],
  ])('traduit le refus du code %i', (status, corps, attendu) => {
    const resultat = verifier();
    repondre('/api/assurance-iard-api/connexion/verification', corps, status);
    expect(resultat()).toEqual(attendu);
    expect(service.jeton()).toBeNull();
  });

  it.each([
    [200, 'code-renvoye'],
    [404, 'verification-perdue'],
    [500, 'erreur-technique'],
  ])('traduit le renvoi du code %i', (status, attendu) => {
    const resultat = renvoyer();
    repondre('/api/assurance-iard-api/connexion/renvoi-code', {}, status);
    expect(resultat()).toBe(attendu);
  });

  describe('session', () => {
    const valide = (): (() => boolean | undefined) => {
      let resultat: boolean | undefined;
      service.sessionValide().subscribe((r) => (resultat = r));
      return () => resultat;
    };

    it("n'appelle pas le backend sans jeton", () => {
      expect(valide()()).toBe(false);
    });

    it('confirme une session valable et retrouve son e-mail', () => {
      sessionStorage.setItem('assureasy.session', 'session');
      const resultat = valide();
      repondre('/api/assurance-iard-api/session', { email: 'marie@exemple.fr', profil: 'GESTIONNAIRE' }, 200);

      expect(resultat()).toBe(true);
      expect(service.emailClient()).toBe('marie@exemple.fr');
      expect(service.profilClient()).toBe('GESTIONNAIRE');
    });

    it('oublie une session refusée par le backend', () => {
      sessionStorage.setItem('assureasy.session', 'session');
      const resultat = valide();
      repondre('/api/assurance-iard-api/session', {}, 401);

      expect(resultat()).toBe(false);
      expect(service.jeton()).toBeNull();
    });

    it('garde le jeton si le service est indisponible', () => {
      sessionStorage.setItem('assureasy.session', 'session');
      const resultat = valide();
      repondre('/api/assurance-iard-api/session', {}, 503);

      expect(resultat()).toBe(false);
      expect(service.jeton()).toBe('session');
    });

    it.each([204, 500])('oublie la session à la déconnexion, même si le backend répond %i', (status) => {
      sessionStorage.setItem('assureasy.session', 'session');
      service.emailClient.set('marie@exemple.fr');
      service.deconnecter().subscribe();
      const requete = http.expectOne('/api/assurance-iard-api/session');
      expect(requete.request.method).toBe('DELETE');
      requete.flush(null, { status, statusText: String(status) });

      expect(service.jeton()).toBeNull();
      expect(service.emailClient()).toBeNull();
      expect(service.profilClient()).toBeNull();
    });
  });
});
