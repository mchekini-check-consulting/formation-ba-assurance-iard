import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { GARANTIES_A03, PROJET_A03 } from '../../core/model/avenant.exemples';
import { ModifierGaranties } from './modifier-garanties';

const BASE = '/api/assurance-iard-api/contrats/AE-RECETTE-A03';

describe('ModifierGaranties (US-23, AC1, AC2, AC3, AC6)', () => {
  let fixture: ComponentFixture<ModifierGaranties>;
  let page: HTMLElement;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ numero: 'AE-RECETTE-A03' })) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(ModifierGaranties);
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const charger = (garanties = GARANTIES_A03): void => {
    http.expectOne(`${BASE}/garanties`).flush(garanties);
    fixture.detectChanges();
  };
  const cases = (): HTMLInputElement[] => Array.from(page.querySelectorAll<HTMLInputElement>('.garantie input'));
  const continuer = (): HTMLButtonElement =>
    Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes('Continuer'))!;

  it('AC1 : affiche mes garanties et celles que je peux ajouter, avec leur prix, et mon prénom', () => {
    expect(page.textContent).toContain('Chargement de vos garanties');
    charger();
    expect(page.querySelector('h1')?.textContent).toContain('Modifier mes garanties');
    expect(page.textContent).toContain('Camille, choisissez');
    expect(page.textContent).not.toContain('Durand');
    const lignes = Array.from(page.querySelectorAll('.garantie')).map((li) => li.textContent?.replace(/\s+/g, ' '));
    expect(lignes[0]).toContain('Responsabilité civile');
    expect(lignes[0]).toContain('12,00 € TTC / mois');
    expect(lignes[1]).toContain('Dans mon contrat');
    expect(lignes[2]).toContain('+ 3,50 € TTC / mois');
    expect(cases().map((c) => c.checked)).toEqual([true, true, false]);
  });

  it('AC2 : la responsabilité civile n’est pas modifiable et la raison est expliquée', () => {
    charger();
    expect(cases()[0].disabled).toBe(true);
    expect(page.querySelector('.garantie__badge')?.textContent).toContain('Obligatoire');
    expect(page.querySelector('#raison-RC')?.textContent).toContain('obligatoire');
    expect(cases()[0].getAttribute('aria-describedby')).toBe('raison-RC');
  });

  it('« Continuer » est désactivé tant qu’aucune garantie n’a changé ; la nouvelle cotisation se calcule en direct', () => {
    charger();
    expect(continuer().disabled).toBe(true);
    expect(page.textContent).toContain('Cochez ou décochez au moins une garantie');

    cases()[2].click();
    cases()[1].click();
    fixture.detectChanges();

    expect(continuer().disabled).toBe(false);
    const valeurs = Array.from(page.querySelectorAll('dd')).map((dd) => dd.textContent?.replace(/\s+/g, ' ').trim());
    expect(valeurs).toEqual(['47,50 € TTC / mois', '48,50 € TTC / mois']);
  });

  it('AC3 : « Continuer » prépare l’avenant puis ouvre « Relire et accepter l’avenant »', () => {
    charger();
    cases()[2].click();
    cases()[1].click();
    fixture.detectChanges();
    continuer().click();

    const requete = http.expectOne(`${BASE}/avenants`);
    expect(requete.request.body).toEqual({ garanties: ['RC', 'ASSISTANCE_0KM'] });
    requete.flush(PROJET_A03);

    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/contrats', 'AE-RECETTE-A03', 'avenant']);
  });

  it('un refus du serveur s’affiche en message clair', () => {
    charger();
    cases()[2].click();
    fixture.detectChanges();
    continuer().click();
    http.expectOne(`${BASE}/avenants`).flush(
      { detail: 'Une des garanties demandées n’est pas proposée pour ce contrat.' },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('n’est pas proposée');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('AC5 : un avenant déjà en attente est signalé (il sera remplacé)', () => {
    charger({ ...GARANTIES_A03, statut: 'AVENANT_EN_ATTENTE', avenantEnAttente: PROJET_A03 });
    expect(page.querySelector('.avenant__note')?.textContent).toContain('AV-2026-000009');
    expect(page.querySelector('.avenant__note')?.textContent).toContain('remplacé');
  });

  it('AC6 : un contrat qui n’est pas actif affiche le message du serveur, sans garantie', () => {
    http.expectOne(`${BASE}/garanties`).flush(
      { detail: 'Les garanties de ce contrat ne peuvent pas être modifiées dans sa situation actuelle.' },
      { status: 422, statusText: 'Unprocessable' },
    );
    fixture.detectChanges();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('ne peuvent pas être modifiées');
    expect(cases()).toHaveLength(0);
  });
});
