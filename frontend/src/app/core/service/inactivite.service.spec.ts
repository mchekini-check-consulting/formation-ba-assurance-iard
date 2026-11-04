import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AnnonceService } from './annonce.service';
import { DELAI_INACTIVITE_MS, DELAI_PROLONGATION_MS, InactiviteService } from './inactivite.service';

describe('InactiviteService', () => {
  let service: InactiviteService;
  let http: HttpTestingController;
  let router: Router;

  const agir = (): void => {
    document.dispatchEvent(new Event('click'));
  };

  const repondreDeconnexion = (): void => {
    http.expectOne((r) => r.method === 'DELETE').flush(null, { status: 204, statusText: 'No Content' });
  };

  const annonce = (): string | null => TestBed.inject(AnnonceService).prendre();

  beforeEach(() => {
    vi.useFakeTimers({ now: new Date('2026-11-04T10:00:00Z') });
    sessionStorage.setItem('assureasy.session', 'session');
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(InactiviteService);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    service.surveiller();
  });

  afterEach(() => {
    // Sans jeton, le prochain contrôle arrête la surveillance et libère ses écouteurs.
    sessionStorage.clear();
    vi.advanceTimersByTime(30 * 1000);
    http.verify();
    vi.useRealTimers();
  });

  it("déconnecte après 30 minutes sans activité et l'annonce sur l'accueil", () => {
    vi.advanceTimersByTime(DELAI_INACTIVITE_MS + 30 * 1000);
    repondreDeconnexion();

    expect(sessionStorage.getItem('assureasy.session')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
    expect(annonce()).toContain('30 minutes d’inactivité');
  });

  it('ne déconnecte pas avant 30 minutes', () => {
    vi.advanceTimersByTime(DELAI_INACTIVITE_MS - 60 * 1000);

    http.expectNone((r) => r.method === 'DELETE');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it("repart de zéro à chaque activité et prolonge la session auprès du backend", () => {
    vi.advanceTimersByTime(DELAI_PROLONGATION_MS);
    agir();
    http.expectOne((r) => r.method === 'GET').flush({ email: 'marie@exemple.fr' });

    vi.advanceTimersByTime(DELAI_INACTIVITE_MS - 60 * 1000);

    http.expectNone((r) => r.method === 'DELETE');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('ne sollicite pas le backend à chaque clic', () => {
    agir();
    agir();

    http.expectNone((r) => r.method === 'GET');
  });

  it('déconnecte si le backend a déjà refusé la session', () => {
    vi.advanceTimersByTime(DELAI_PROLONGATION_MS);
    agir();
    http.expectOne((r) => r.method === 'GET').flush({}, { status: 401, statusText: 'Unauthorized' });
    repondreDeconnexion();

    expect(router.navigate).toHaveBeenCalledWith(['/']);
    expect(annonce()).toContain('30 minutes d’inactivité');
  });

  it('reste connecté si le service est momentanément indisponible', () => {
    vi.advanceTimersByTime(DELAI_PROLONGATION_MS);
    agir();
    http.expectOne((r) => r.method === 'GET').flush({}, { status: 503, statusText: 'Unavailable' });

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it("déconnecte au retour sur l'onglet après une longue absence", () => {
    vi.setSystemTime(new Date('2026-11-04T11:00:00Z'));
    document.dispatchEvent(new Event('visibilitychange'));
    repondreDeconnexion();

    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('déconnecte à la première action après une mise en veille', () => {
    vi.setSystemTime(new Date('2026-11-04T11:00:00Z'));
    agir();
    repondreDeconnexion();

    expect(annonce()).toContain('30 minutes d’inactivité');
  });

  it("s'arrête après une déconnexion manuelle, sans annonce d'inactivité", () => {
    sessionStorage.clear();
    vi.advanceTimersByTime(DELAI_INACTIVITE_MS + 60 * 1000);

    http.expectNone((r) => r.method === 'DELETE');
    expect(annonce()).toBeNull();
  });

  it('ne lance pas deux surveillances', () => {
    service.surveiller();
    vi.advanceTimersByTime(DELAI_INACTIVITE_MS + 30 * 1000);
    repondreDeconnexion();

    expect(router.navigate).toHaveBeenCalledTimes(1);
  });
});
