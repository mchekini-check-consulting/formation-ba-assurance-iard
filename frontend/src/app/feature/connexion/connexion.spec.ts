import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Connexion } from './connexion';

describe('Connexion', () => {
  let fixture: ComponentFixture<Connexion>;
  let page: HTMLElement;
  let router: Router;
  let http: HttpTestingController;

  const creer = (parametres: Record<string, string> = {}): void => {
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
    fixture = TestBed.createComponent(Connexion);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };

  const bouton = (): HTMLButtonElement => page.querySelector<HTMLButtonElement>('button[type="submit"]')!;

  const saisir = (selecteur: string, valeur: string): void => {
    const champ = page.querySelector<HTMLInputElement>(selecteur)!;
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  const soumettre = (email = 'marie@exemple.fr', motDePasse = 'Abcdefg1'): void => {
    saisir('#email', email);
    saisir('#mot-de-passe', motDePasse);
    bouton().click();
    fixture.detectChanges();
  };

  const repondre = (corps: object, status: number): void => {
    http.expectOne('/api/connexion').flush(corps, { status, statusText: String(status) });
    fixture.detectChanges();
  };

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('affiche un formulaire vide, sans aucune pré-sélection', () => {
    creer();
    expect(page.querySelector('h1')?.textContent).toContain('Connexion');
    expect(page.querySelector<HTMLInputElement>('#email')?.value).toBe('');
    expect(page.querySelector<HTMLInputElement>('#mot-de-passe')?.value).toBe('');
    expect(page.querySelector('input[type="checkbox"], input[type="radio"]')).toBeNull();
  });

  it('demande les champs manquants sans appeler le backend', () => {
    creer();
    bouton().click();
    fixture.detectChanges();
    expect(page.querySelector('#erreur-email')?.textContent).toContain('Saisissez une adresse e-mail valide.');
    expect(document.activeElement).toBe(page.querySelector('#email'));

    soumettre('marie@exemple.fr', '');
    expect(page.querySelector('#erreur-mot-de-passe')?.textContent).toContain('Saisissez votre mot de passe.');
    expect(document.activeElement).toBe(page.querySelector('#mot-de-passe'));
  });

  it("passe à l'écran de vérification, en conservant la page demandée (AC1, AC6)", () => {
    creer({ retour: '/espace-client/documents' });
    soumettre();
    expect(bouton().disabled).toBe(true);
    repondre({ verification: 'v1', emailMasque: 'm***@exemple.fr' }, 200);
    expect(router.navigate).toHaveBeenCalledWith(['/double-authentification'], {
      queryParams: { retour: '/espace-client/documents' },
    });
  });

  it('passe à la vérification sans page demandée', () => {
    creer();
    soumettre();
    repondre({ verification: 'v1', emailMasque: 'm***@exemple.fr' }, 200);
    expect(router.navigate).toHaveBeenCalledWith(['/double-authentification'], { queryParams: {} });
  });

  it('indique une erreur sans préciser le champ, puis la retire à la saisie (AC4)', () => {
    creer();
    soumettre();
    repondre({}, 401);
    expect(page.querySelector('.alerte')?.textContent).toContain('E-mail ou mot de passe incorrect.');
    expect(router.navigate).not.toHaveBeenCalled();

    saisir('#mot-de-passe', 'Autre1234');
    expect(page.querySelector('.alerte')).toBeNull();
  });

  it('désactive le bouton pendant le blocage, puis le réactive (AC5)', () => {
    vi.useFakeTimers({ now: new Date('2026-11-04T10:00:00Z') });
    creer();
    soumettre();
    repondre({ bloqueJusquA: '2026-11-04T10:15:00Z' }, 423);
    expect(page.querySelector('.alerte')?.textContent).toContain('Compte bloqué 15 minutes.');
    expect(bouton().disabled).toBe(true);

    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    http.expectNone('/api/connexion');

    vi.advanceTimersByTime(15 * 60 * 1000);
    fixture.detectChanges();
    expect(page.querySelector('.alerte')).toBeNull();
    expect(bouton().disabled).toBe(false);
  });

  it("permet d'essayer une autre adresse pendant le blocage", () => {
    creer();
    soumettre();
    repondre({ bloqueJusquA: new Date(Date.now() + 60000).toISOString() }, 423);
    saisir('#email', 'autre@exemple.fr');
    expect(bouton().disabled).toBe(false);
  });

  describe('mot de passe oublié (US-07)', () => {
    const oublie = (): HTMLButtonElement =>
      Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Mot de passe oublié')!;

    const cliquerOublie = (): void => {
      oublie().click();
      fixture.detectChanges();
    };

    const repondreOubli = (status: number): void => {
      http.expectOne('/api/mot-de-passe/oubli').flush(null, { status, statusText: String(status) });
      fixture.detectChanges();
    };

    it('ne soumet pas le formulaire de connexion', () => {
      creer();
      expect(oublie().type).toBe('button');
    });

    it('annonce l’envoi du lien sans dire si le compte existe (AC1, RG1)', () => {
      creer();
      saisir('#email', ' marie@exemple.fr ');
      cliquerOublie();
      expect(oublie().disabled).toBe(true);
      repondreOubli(204);

      expect(page.querySelector('.information')?.textContent).toContain(
        'Si un compte existe, un lien de réinitialisation a été envoyé.',
      );
      expect(oublie().disabled).toBe(false);
    });

    it.each([[''], ['prenom.nom']])('invite à saisir une adresse pour « %s » (AC2)', (email) => {
      creer();
      saisir('#email', email);
      cliquerOublie();

      expect(page.querySelector('.alerte')?.textContent).toContain('Saisissez votre adresse e-mail');
      expect(document.activeElement).toBe(page.querySelector('#email'));
      expect(page.querySelector('.information')).toBeNull();
    });

    it('retire les messages quand on modifie l’adresse', () => {
      creer();
      cliquerOublie();
      saisir('#email', 'marie@exemple.fr');
      expect(page.querySelector('.alerte')).toBeNull();

      cliquerOublie();
      repondreOubli(204);
      saisir('#email', 'autre@exemple.fr');
      expect(page.querySelector('.information')).toBeNull();
    });

    it('reste possible pendant le blocage du compte', () => {
      creer();
      soumettre();
      repondre({ bloqueJusquA: new Date(Date.now() + 60000).toISOString() }, 423);
      expect(oublie().disabled).toBe(false);

      cliquerOublie();
      repondreOubli(204);

      expect(page.querySelector('.information')).not.toBeNull();
      expect(bouton().disabled).toBe(true);
    });

    it('prévient si le service est indisponible', () => {
      creer();
      saisir('#email', 'marie@exemple.fr');
      cliquerOublie();
      repondreOubli(500);

      expect(page.querySelector('.alerte')?.textContent).toContain('Le service est momentanément indisponible.');
      expect(page.querySelector('.information')).toBeNull();
    });
  });

  it('prévient si le service est indisponible', () => {
    creer();
    soumettre();
    repondre({}, 500);
    expect(page.querySelector('.alerte')?.textContent).toContain('Le service est momentanément indisponible.');
    expect(bouton().disabled).toBe(false);
  });
});
