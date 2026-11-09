import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { ReinitialisationMotDePasse } from './reinitialisation-mot-de-passe';

describe('ReinitialisationMotDePasse', () => {
  let fixture: ComponentFixture<ReinitialisationMotDePasse>;
  let page: HTMLElement;
  let http: HttpTestingController;

  const creer = (parametres: Record<string, string> = { jeton: 'jeton' }): void => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(parametres) } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ReinitialisationMotDePasse);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };

  const repondre = (url: string, status: number): void => {
    http.expectOne(url).flush(null, { status, statusText: String(status) });
    fixture.detectChanges();
  };

  const ouvrirLienValable = (): void => {
    creer();
    repondre('/api/assurance-iard-api/mot-de-passe/lien', 204);
  };

  const saisir = (valeur: string): void => {
    const champ = page.querySelector<HTMLInputElement>('#mot-de-passe')!;
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  const enregistrer = (): void => {
    page.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
  };

  const liens = (): (string | null)[] =>
    Array.from(page.querySelectorAll('main a')).map((a) => a.getAttribute('href'));

  afterEach(() => http.verify());

  it('vérifie le lien dès l’ouverture', () => {
    creer();
    expect(page.textContent).toContain('Vérification du lien en cours');
    expect(page.querySelector('#mot-de-passe')).toBeNull();
    repondre('/api/assurance-iard-api/mot-de-passe/lien', 204);
    expect(page.querySelector<HTMLInputElement>('#mot-de-passe')?.value).toBe('');
  });

  it('applique les règles de la création de compte (AC4)', () => {
    ouvrirLienValable();
    const etats = (): string[] =>
      Array.from(page.querySelectorAll('.regle')).map((r) => (r.classList.contains('regle--ok') ? 'ok' : 'ko'));
    expect(etats()).toEqual(['ko', 'ko', 'ko']);

    saisir('Nouveau123');
    expect(etats()).toEqual(['ok', 'ok', 'ok']);

    saisir('faible');
    enregistrer();
    expect(page.querySelector('.erreur')?.textContent).toContain('ne respecte pas toutes les règles');
    expect(document.activeElement).toBe(page.querySelector('#mot-de-passe'));
  });

  it('confirme le changement puis propose la connexion (AC4)', () => {
    ouvrirLienValable();
    saisir('Nouveau123');
    enregistrer();
    const requete = http.expectOne('/api/assurance-iard-api/mot-de-passe/reinitialisation');
    expect(requete.request.body).toEqual({ jeton: 'jeton', motDePasse: 'Nouveau123' });
    requete.flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();

    expect(page.querySelector('h1')?.textContent).toContain('Mot de passe modifié');
    expect(liens()).toEqual(['/connexion']);
  });

  it('explique un lien expiré ou déjà utilisé et permet d’en redemander un (AC5)', () => {
    creer();
    repondre('/api/assurance-iard-api/mot-de-passe/lien', 410);

    expect(page.querySelector('h1')?.textContent).toContain('Lien expiré ou déjà utilisé');
    expect(page.querySelector('#mot-de-passe')).toBeNull();
    expect(page.querySelector('main a')?.textContent).toContain('Demander un nouveau lien');
    expect(liens()).toEqual(['/connexion']);
  });

  it('traite une adresse sans jeton comme un lien invalide, sans appeler le backend', () => {
    creer({});
    fixture.detectChanges();
    expect(page.querySelector('h1')?.textContent).toContain('Lien expiré ou déjà utilisé');
  });

  it('explique que le lien a expiré pendant la saisie (AC5)', () => {
    ouvrirLienValable();
    saisir('Nouveau123');
    enregistrer();
    repondre('/api/assurance-iard-api/mot-de-passe/reinitialisation', 410);

    expect(page.querySelector('h1')?.textContent).toContain('Lien expiré ou déjà utilisé');
  });

  it('prévient si le service est indisponible à l’ouverture', () => {
    creer();
    repondre('/api/assurance-iard-api/mot-de-passe/lien', 500);

    expect(page.querySelector('.alerte')?.textContent).toContain('Le service est momentanément indisponible.');
  });

  it('garde la saisie si le service est indisponible à l’enregistrement', () => {
    ouvrirLienValable();
    saisir('Nouveau123');
    enregistrer();
    repondre('/api/assurance-iard-api/mot-de-passe/reinitialisation', 500);

    expect(page.querySelector('.alerte')?.textContent).toContain("Votre mot de passe n'a pas été modifié");
    expect(page.querySelector('#mot-de-passe')).not.toBeNull();
  });
});
