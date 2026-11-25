import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { DevisAutoVehicule } from './devis-auto-vehicule';

const fournirRoute = (mode: string | null) => ({
  provide: ActivatedRoute,
  useValue: {
    snapshot: { queryParamMap: { get: (key: string) => (key === 'mode' ? mode : null) } },
    queryParamMap: of({ get: (key: string) => (key === 'mode' ? mode : null) }),
  },
});

describe('DevisAutoVehicule', () => {
  let http: HttpTestingController;

  beforeEach(() => history.pushState({}, ''));

  const creerComposant = (mode: string | null = null) => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), fournirRoute(mode)],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DevisAutoVehicule);
    fixture.detectChanges();
    http.expectOne((req) => req.url.includes('referentiels/vehicules')).flush([]);
    fixture.detectChanges();
    return fixture;
  };

  afterEach(() => http.verify());

  // ─── MODE PLAQUE ──────────────────────────────────────────────────────────

  it('mode=plaque : affiche le titre et le champ immatriculation', () => {
    const fixture = creerComposant('plaque');
    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('Retrouvez votre véhicule');
    expect(page.querySelector('#plaque')).toBeTruthy();
    expect(page.querySelector('form')).toBeTruthy();
  });

  it('sans parametre mode : comportement plaque par defaut', () => {
    const fixture = creerComposant(null);
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('#plaque')).toBeTruthy();
  });

  it('mode=plaque : les tuiles de marques ne sont pas affichees', () => {
    const fixture = creerComposant('plaque');
    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).not.toContain('Renault');
    expect(page.textContent).not.toContain('Peugeot');
    expect(page.textContent).not.toContain('Citroën');
  });

  it('mode=plaque : affiche le lien "Je ne connais pas ma plaque"', () => {
    const fixture = creerComposant('plaque');
    const page = fixture.nativeElement as HTMLElement;
    const lien = Array.from(page.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Je ne connais pas ma plaque'),
    );
    expect(lien).toBeTruthy();
  });

  it("mode=plaque : affiche un message d'erreur si la plaque est vide a la soumission", () => {
    const fixture = creerComposant('plaque');
    const form = (fixture.nativeElement as HTMLElement).querySelector('form')!;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain("Saisissez votre plaque d'immatriculation.");
  });

  it('mode=plaque : "Je ne connais pas ma plaque" appelle router.navigate avec mode=marque', () => {
    const fixture = creerComposant('plaque');
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const lien = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Je ne connais pas ma plaque'),
    ) as HTMLButtonElement;
    lien.click();

    expect(router.navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { mode: 'marque' } }));
  });

  it('affiche "Vehicule trouve" apres un resultat TROUVE', () => {
    const fixture = creerComposant('plaque');
    const input = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#plaque')!;
    input.value = 'AB-123-CD';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    http.expectOne((req) => req.url.includes('recherche-vehicule')).flush({
      statut: 'TROUVE',
      immatriculation: 'AB-123-CD',
      refVehiculeId: 1,
      marque: 'Peugeot',
      modele: '308',
      energie: 'ESSENCE',
    });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Peugeot');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('308');
  });

  it('affiche un message INCONNU pour une plaque non reconnue', () => {
    const fixture = creerComposant('plaque');
    const input = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#plaque')!;
    input.value = 'XX-000-XX';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    http.expectOne((req) => req.url.includes('recherche-vehicule')).flush({ statut: 'INCONNU', immatriculation: 'XX-000-XX' });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('pas reconnu');
  });

  // ─── MODE MARQUE ──────────────────────────────────────────────────────────

  it('mode=marque : affiche les tuiles de marques directement', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), fournirRoute('marque')],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DevisAutoVehicule);
    fixture.detectChanges();
    http.expectOne((req) => req.url.includes('referentiels/vehicules')).flush([
      { marque: 'Peugeot', modeles: [{ id: 1, modele: '308', energie: 'ESSENCE', version: null, annee: 2021, valeurNeuve: 18500 }] },
    ]);
    fixture.detectChanges();

    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Renault');
    expect(texte).toContain('Peugeot');
    expect(texte).toContain('Citroën');
    expect(texte).toContain('Toyota');
    expect(texte).toContain('Ford');
    expect(texte).toContain('Autre marque');
    expect(texte).not.toContain('Tesla');
  });

  it('mode=marque : clic sur "Autre marque" affiche le champ de recherche', () => {
    const fixture = creerComposant('marque');
    const page = fixture.nativeElement as HTMLElement;

    const tuileAutre = Array.from(page.querySelectorAll('button')).find(b =>
      b.textContent?.includes('Autre marque'),
    ) as HTMLButtonElement;
    tuileAutre.click();
    fixture.detectChanges();

    expect(page.querySelector('#recherche-marque')).toBeTruthy();
  });

  it('mode=marque : suggestions apparaissent après 2 caractères', () => {
    const fixture = creerComposant('marque');
    const page = fixture.nativeElement as HTMLElement;

    const tuileAutre = Array.from(page.querySelectorAll('button')).find(b =>
      b.textContent?.includes('Autre marque'),
    ) as HTMLButtonElement;
    tuileAutre.click();
    fixture.detectChanges();

    const input = page.querySelector<HTMLInputElement>('#recherche-marque')!;
    input.value = 'bm';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(page.textContent).toContain('BMW');
  });

  it('mode=marque : message introuvable si aucune suggestion', () => {
    const fixture = creerComposant('marque');
    const page = fixture.nativeElement as HTMLElement;

    const tuileAutre = Array.from(page.querySelectorAll('button')).find(b =>
      b.textContent?.includes('Autre marque'),
    ) as HTMLButtonElement;
    tuileAutre.click();
    fixture.detectChanges();

    const input = page.querySelector<HTMLInputElement>('#recherche-marque')!;
    input.value = 'zzzzz';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(page.textContent).toContain('Marque introuvable');
  });

  it('mode=marque : clic sur une suggestion ferme la recherche', () => {
    const fixture = creerComposant('marque');
    const page = fixture.nativeElement as HTMLElement;

    const tuileAutre = Array.from(page.querySelectorAll('button')).find(b =>
      b.textContent?.includes('Autre marque'),
    ) as HTMLButtonElement;
    tuileAutre.click();
    fixture.detectChanges();

    const input = page.querySelector<HTMLInputElement>('#recherche-marque')!;
    input.value = 'bm';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const suggestion = Array.from(page.querySelectorAll('.suggestion')).find(el =>
      el.textContent?.includes('BMW'),
    ) as HTMLElement;
    suggestion.click();
    fixture.detectChanges();

    expect(page.querySelector('#recherche-marque')).toBeNull();
  });

  it('mode=marque : masque le champ immatriculation', () => {
    const fixture = creerComposant('marque');
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('#plaque')).toBeNull();
    expect(page.querySelector('form')).toBeNull();
  });

  it('mode=marque : affiche le lien "Je connais ma plaque"', () => {
    const fixture = creerComposant('marque');
    const page = fixture.nativeElement as HTMLElement;
    const lien = Array.from(page.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Je connais ma plaque'),
    );
    expect(lien).toBeTruthy();
  });

  it('mode=marque : "Je connais ma plaque" appelle router.navigate avec mode=plaque', () => {
    const fixture = creerComposant('marque');
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const lien = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Je connais ma plaque'),
    ) as HTMLButtonElement;
    lien.click();

    expect(router.navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { mode: 'plaque' } }));
  });
});
