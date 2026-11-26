import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AVENANT_A03, GARANTIES_A03, PROJET_A03 } from '../../core/model/avenant.exemples';
import { RelireAvenant } from './relire-avenant';

const BASE = '/api/assurance-iard-api/contrats/AE-RECETTE-A03';
const EN_ATTENTE = { ...GARANTIES_A03, statut: 'AVENANT_EN_ATTENTE' as const, avenantEnAttente: PROJET_A03 };

describe('RelireAvenant (US-23, AC3, AC4, AC5, RG1, RG4)', () => {
  let fixture: ComponentFixture<RelireAvenant>;
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
    fixture = TestBed.createComponent(RelireAvenant);
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const charger = (garanties: typeof GARANTIES_A03 = EN_ATTENTE): void => {
    http.expectOne(`${BASE}/garanties`).flush(garanties);
    fixture.detectChanges();
  };
  const texte = (): string => page.textContent?.replace(/\s+/g, ' ') ?? '';
  const bouton = (libelle: string): HTMLButtonElement =>
    Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes(libelle))!;
  const caseAcceptation = (): HTMLInputElement => page.querySelector('.avenant__acceptation input')!;

  it('AC3 : ancienne et nouvelle prime, date d’effet, application et projet d’avenant', () => {
    charger();
    expect(texte()).toContain('Camille, voici ce qui change');
    const valeurs = Array.from(page.querySelectorAll('dd')).map((dd) => dd.textContent?.replace(/\s+/g, ' ').trim());
    expect(valeurs[0]).toBe('47,50 € TTC / mois (570,00 € TTC / an)');
    expect(valeurs[1]).toBe('48,50 € TTC / mois (582,00 € TTC / an)');
    expect(valeurs[2]).toBe('27/11/2026 (lendemain de votre acceptation)');
    expect(texte()).toContain('première échéance après la date d\'effet (05/12/2026), sans prorata');
    expect(texte()).toContain("Projet d'avenant AV-2026-000009");
    expect(texte()).toContain('Assistance dès 0 km : + 3,50 € TTC / mois');
    expect(texte()).toContain('Protection juridique : − 2,50 € TTC / mois');
  });

  it('AC3 : documents à relire (IPID de la formule, conditions générales)', () => {
    charger();
    const ipid = page.querySelector<HTMLAnchorElement>('a[href*="ipid"]');
    expect(ipid?.getAttribute('href')).toBe('/api/assurance-iard-api/documents-contrat/ipid/TR');
    expect(texte()).toContain('Conditions générales');
  });

  it('RG4 : le conseil mis à jour s’affiche quand la couverture diminue', () => {
    charger();
    expect(page.querySelector('.avenant__conseil')?.textContent).toContain('Protection juridique');
    expect(page.querySelector('.avenant__conseil')?.textContent).toContain('L521-4');
  });

  it('RG4 : pas de conseil si la couverture ne diminue pas', () => {
    charger({ ...EN_ATTENTE, avenantEnAttente: { ...PROJET_A03, reductionCouverture: false, conseils: [] } });
    expect(page.querySelector('.avenant__conseil')).toBeNull();
  });

  it('RG1 : la case n’est jamais pré-cochée et « Accepter l’avenant » reste désactivé tant qu’elle ne l’est pas', () => {
    charger();
    expect(caseAcceptation().checked).toBe(false);
    expect(bouton("Accepter l'avenant").disabled).toBe(true);

    caseAcceptation().click();
    fixture.detectChanges();

    expect(bouton("Accepter l'avenant").disabled).toBe(false);
  });

  it('AC4 : accepter ouvre « Avenant accepté » sans recharger la page', () => {
    charger();
    caseAcceptation().click();
    fixture.detectChanges();
    bouton("Accepter l'avenant").click();

    const requete = http.expectOne(`${BASE}/avenants/en-attente/acceptation`);
    expect(requete.request.body).toEqual({ accepte: true });
    const acceptation = { avenant: { ...AVENANT_A03, statut: 'ACCEPTE' }, disponible: true, emailEnvoye: true, versionConditionsParticulieres: 2 };
    requete.flush(acceptation);

    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/contrats', 'AE-RECETTE-A03', 'avenant-accepte'], {
      state: { acceptation },
    });
  });

  it('AC4 : un échec d’acceptation s’affiche en message clair', () => {
    charger();
    caseAcceptation().click();
    fixture.detectChanges();
    bouton("Accepter l'avenant").click();
    http.expectOne(`${BASE}/avenants/en-attente/acceptation`).flush(null, { status: 500, statusText: 'KO' });
    fixture.detectChanges();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain("L'opération n'a pas pu aboutir");
  });

  it('AC5 : abandonner l’avenant met l’écran à jour (contrat aux conditions actuelles)', () => {
    charger();
    bouton("Abandonner l'avenant").click();
    http.expectOne(`${BASE}/avenants/en-attente/abandon`).flush({ ...AVENANT_A03, statut: 'ABANDONNE' });
    fixture.detectChanges();

    expect(page.querySelector('[role="status"]')?.textContent).toContain('AV-2026-000009 est abandonné');
    expect(texte()).toContain("Aucun avenant n'attend votre réponse");
    expect(page.querySelector('.avenant__acceptation')).toBeNull();
  });

  it('sans avenant en attente, propose de revenir au contrat ou de modifier ses garanties', () => {
    charger(GARANTIES_A03);
    expect(texte()).toContain("Aucun avenant n'attend votre réponse");
    expect(texte()).toContain('Modifier mes garanties');
  });

  it('AC6 : contrat non modifiable, message du serveur', () => {
    http.expectOne(`${BASE}/garanties`).flush(
      { detail: 'Les garanties de ce contrat ne peuvent pas être modifiées dans sa situation actuelle.' },
      { status: 422, statusText: 'Unprocessable' },
    );
    fixture.detectChanges();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('ne peuvent pas être modifiées');
  });
});
