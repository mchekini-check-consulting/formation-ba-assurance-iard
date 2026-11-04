import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { ConnexionService } from '../../core/service/connexion.service';
import { DoubleAuthentification } from './double-authentification';

describe('DoubleAuthentification', () => {
  let fixture: ComponentFixture<DoubleAuthentification>;
  let page: HTMLElement;
  let router: Router;
  let http: HttpTestingController;

  const creer = (parametres: Record<string, string> = {}): void => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(parametres) } } },
      ],
    });
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    TestBed.inject(ConnexionService).verification.set({ verification: 'v1', emailMasque: 'm***@exemple.fr' });
    fixture = TestBed.createComponent(DoubleAuthentification);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };

  const champ = (): HTMLInputElement => page.querySelector<HTMLInputElement>('#code')!;
  const valider = (): HTMLButtonElement => page.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const renvoyer = (): HTMLButtonElement =>
    Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Renvoyer un code')!;

  const saisir = (valeur: string): void => {
    champ().value = valeur;
    champ().dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  const soumettre = (code = '123456'): void => {
    saisir(code);
    valider().click();
    fixture.detectChanges();
  };

  const repondre = (url: string, corps: object, status: number): void => {
    http.expectOne(url).flush(corps, { status, statusText: String(status) });
    fixture.detectChanges();
  };

  afterEach(() => http.verify());

  it("affiche l'adresse masquée et un champ vide (AC1)", () => {
    creer();
    expect(page.querySelector('h1')?.textContent).toContain('Double authentification');
    expect(page.querySelector('strong')?.textContent).toBe('m***@exemple.fr');
    expect(champ().value).toBe('');
  });

  it('ne garde que 6 chiffres', () => {
    creer();
    saisir('12a34567');
    expect(champ().value).toBe('123456');
  });

  it('demande les 6 chiffres sans appeler le backend', () => {
    creer();
    soumettre('123');
    expect(page.querySelector('#erreur-code')?.textContent).toContain('Saisissez les 6 chiffres du code.');
    expect(document.activeElement).toBe(champ());
  });

  it('ouvre le tableau de bord avec un code valide (AC2)', () => {
    creer();
    soumettre();
    expect(valider().disabled).toBe(true);
    repondre('/api/connexion/verification', { jeton: 'session', email: 'marie@exemple.fr' }, 200);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/espace-client');
  });

  it('ouvre la page initialement demandée (AC2, AC6)', () => {
    creer({ retour: '/espace-client/documents' });
    soumettre();
    repondre('/api/connexion/verification', { jeton: 'session', email: 'marie@exemple.fr' }, 200);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/espace-client/documents');
  });

  it("n'accepte pas de page de retour extérieure au site", () => {
    creer({ retour: 'https://pirate.example' });
    soumettre();
    repondre('/api/connexion/verification', { jeton: 'session', email: 'marie@exemple.fr' }, 200);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/espace-client');
  });

  it('indique un code incorrect et les essais restants (AC3)', () => {
    creer();
    soumettre();
    repondre('/api/connexion/verification', { essaisRestants: 2 }, 401);
    expect(page.querySelector('.alerte')?.textContent).toContain('Code incorrect. Il vous reste 2 essais.');

    soumettre();
    repondre('/api/connexion/verification', { essaisRestants: 1 }, 401);
    expect(page.querySelector('.alerte')?.textContent).toContain('Il vous reste 1 essai.');
    expect(valider().disabled).toBe(false);
  });

  it('impose un nouveau code après 3 erreurs, puis permet de réessayer (AC3, RG2)', () => {
    creer();
    soumettre();
    repondre('/api/connexion/verification', { motif: 'essais-epuises' }, 409);
    expect(page.querySelector('.alerte')?.textContent).toContain('vous devez demander un nouveau code');
    expect(valider().disabled).toBe(true);

    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    http.expectNone('/api/connexion/verification');

    renvoyer().click();
    fixture.detectChanges();
    const requete = http.expectOne('/api/connexion/renvoi-code');
    expect(requete.request.body).toEqual({ verification: 'v1' });
    requete.flush({ verification: 'v1', emailMasque: 'm***@exemple.fr' });
    fixture.detectChanges();

    expect(page.querySelector('.alerte')).toBeNull();
    expect(valider().disabled).toBe(false);
    expect(champ().value).toBe('');
    expect(page.querySelector('[role="status"]')?.textContent).toContain('Un nouveau code vient de vous être envoyé.');

    page.querySelector<HTMLButtonElement>('[aria-label="Fermer le message"]')!.click();
    fixture.detectChanges();
    expect(page.querySelector('[role="status"]')).toBeNull();
  });

  it('impose un nouveau code après 10 minutes (RG2)', () => {
    creer();
    soumettre();
    repondre('/api/connexion/verification', { motif: 'code-expire' }, 409);
    expect(page.querySelector('.alerte')?.textContent).toContain('Ce code a expiré.');
    expect(valider().disabled).toBe(true);
  });

  it('renvoie à la connexion si la vérification n’existe plus', () => {
    creer();
    soumettre();
    repondre('/api/connexion/verification', {}, 404);
    expect(router.navigate).toHaveBeenCalledWith(['/connexion']);
  });

  it('renvoie à la connexion si le renvoi ne trouve plus la vérification', () => {
    creer();
    renvoyer().click();
    repondre('/api/connexion/renvoi-code', {}, 404);
    expect(router.navigate).toHaveBeenCalledWith(['/connexion']);
  });

  it.each([
    ['la vérification', () => soumettre(), '/api/connexion/verification'],
    ['le renvoi', () => renvoyer().click(), '/api/connexion/renvoi-code'],
  ])('prévient si le service est indisponible pendant %s', (_nom, action, url) => {
    creer();
    action();
    repondre(url, {}, 500);
    expect(page.querySelector('.alerte')?.textContent).toContain('Le service est momentanément indisponible.');
  });
});
