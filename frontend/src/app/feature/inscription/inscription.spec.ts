import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CompteService } from '../../core/service/compte.service';
import { Inscription } from './inscription';

describe('Inscription', () => {
  let fixture: ComponentFixture<Inscription>;
  let page: HTMLElement;
  let router: Router;
  let http: HttpTestingController;

  const repondre = (corps: object, status: number): void => {
    http.expectOne('/api/comptes').flush(corps, { status, statusText: String(status) });
    fixture.detectChanges();
  };

  const boutonCreer = (): HTMLButtonElement =>
    Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Créer mon compte')!;

  const saisir = (selecteur: string, valeur: string): HTMLInputElement => {
    const champ = page.querySelector<HTMLInputElement>(selecteur)!;
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    return champ;
  };

  const valider = (): void => {
    boutonCreer().click();
    fixture.detectChanges();
  };

  const regles = (): string[] =>
    Array.from(page.querySelectorAll('.regle')).map((r) => (r.classList.contains('regle--ok') ? 'ok' : 'ko'));

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(Inscription);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  it('affiche le formulaire de la maquette', () => {
    expect(page.querySelector('h1')?.textContent).toContain('Créer mon compte');
    expect(page.querySelector('label[for="email"]')?.textContent).toContain('Adresse e-mail');
    expect(page.querySelector<HTMLInputElement>('#mot-de-passe')?.type).toBe('password');
    expect(page.textContent).toContain('Déjà client ?');
    expect(page.querySelector('[role="alert"]')).toBeNull();
  });

  it('coche les règles du mot de passe au fil de la saisie (RG2)', () => {
    expect(regles()).toEqual(['ko', 'ko', 'ko']);
    saisir('#mot-de-passe', 'abcdefgh');
    expect(regles()).toEqual(['ok', 'ko', 'ko']);
    saisir('#mot-de-passe', 'Abcdefg1');
    expect(regles()).toEqual(['ok', 'ok', 'ok']);
  });

  it("signale un e-mail invalide et replace le curseur dans le champ", () => {
    const email = saisir('#email', 'prenom.nom');
    saisir('#mot-de-passe', 'Abcdefg1');
    valider();
    expect(page.querySelector('#erreur-email')?.textContent).toContain('Saisissez une adresse e-mail valide.');
    expect(email.getAttribute('aria-invalid')).toBe('true');
    expect(document.activeElement).toBe(email);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('indique les règles à respecter si le mot de passe est trop simple', () => {
    saisir('#email', 'marie@exemple.fr');
    const motDePasse = saisir('#mot-de-passe', 'abc');
    valider();
    expect(page.textContent).toContain('Votre mot de passe ne respecte pas toutes les règles ci-dessus.');
    expect(document.activeElement).toBe(motDePasse);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it("invite à se connecter si l'e-mail est déjà utilisé", () => {
    saisir('#email', 'deja@exemple.fr');
    saisir('#mot-de-passe', 'Abcdefg1');
    valider();
    repondre({ detail: 'Un compte existe déjà avec cette adresse.' }, 409);
    const alerte = page.querySelector('.alerte');
    expect(alerte?.textContent).toContain('Un compte existe déjà avec cette adresse.');
    expect(alerte?.querySelector('a')?.getAttribute('href')).toBe('/connexion');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it("retire l'alerte « déjà utilisé » quand on change d'e-mail", () => {
    saisir('#email', 'deja@exemple.fr');
    saisir('#mot-de-passe', 'Abcdefg1');
    valider();
    repondre({}, 409);
    saisir('#email', 'autre@exemple.fr');
    expect(page.querySelector('.alerte')).toBeNull();
  });

  it('crée le compte, connecte la personne et ouvre la page « Compte créé »', () => {
    saisir('#email', 'marie@exemple.fr');
    saisir('#mot-de-passe', 'Abcdefg1');
    valider();
    expect(boutonCreer().disabled).toBe(true);
    valider();
    repondre({ id: 7, email: 'marie@exemple.fr' }, 201);
    expect(boutonCreer().disabled).toBe(false);
    expect(TestBed.inject(CompteService).emailConnecte()).toBe('marie@exemple.fr');
    expect(router.navigate).toHaveBeenCalledWith(['/compte-cree']);
  });

  it("prévient que le compte n'a pas été créé si le service est indisponible", () => {
    saisir('#email', 'marie@exemple.fr');
    saisir('#mot-de-passe', 'Abcdefg1');
    valider();
    http.expectOne('/api/comptes').error(new ProgressEvent('error'), { status: 0 });
    fixture.detectChanges();
    expect(page.querySelector('.alerte')?.textContent).toContain('Le service est momentanément indisponible.');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it("« Déjà client ? » mène à l'écran de connexion (US-06)", () => {
    expect(page.querySelector('.entete__client a')?.getAttribute('href')).toBe('/connexion');
  });
});
