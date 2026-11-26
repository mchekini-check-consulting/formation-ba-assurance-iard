import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ContexteDeclaration } from '../../core/model/sinistre';
import { BROUILLON_VOL, TYPES_AUTO } from '../../core/model/sinistre.exemples';
import { DeclarerSinistre, MESSAGE_AUCUN_CONTRAT, MESSAGE_ENREGISTRE } from './declarer-sinistre';

const BASE = '/api/assurance-iard-api';
const CONTEXTE: ContexteDeclaration = {
  prenom: 'CAMILLE',
  numeroAssistance: '01 23 45 67 89',
  contrats: [BROUILLON_VOL.contrat],
};

describe('DeclarerSinistre, étape 1 (US-24, AC1 à AC3, AC8, AC10, AC12)', () => {
  let fixture: ComponentFixture<DeclarerSinistre>;
  let page: HTMLElement;
  let http: HttpTestingController;
  let router: Router;

  const creer = (parametres: Record<string, string> = {}, requete: Record<string, string> = {}): void => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap(parametres)),
            snapshot: { queryParamMap: convertToParamMap(requete) },
          },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(TestBed.inject(Location), 'replaceState').mockImplementation(() => undefined);
    fixture = TestBed.createComponent(DeclarerSinistre);
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  };
  const rendre = (): void => fixture.detectChanges();
  const boutons = (): HTMLButtonElement[] => Array.from(page.querySelectorAll<HTMLButtonElement>('button'));
  const bouton = (texte: string): HTMLButtonElement => boutons().find((b) => b.textContent?.includes(texte))!;
  const tuiles = (): HTMLButtonElement[] => Array.from(page.querySelectorAll<HTMLButtonElement>('.tuile'));

  afterEach(() => http.verify());

  it('AC1 : un seul contrat est proposé sans être présélectionné ; champs obligatoires et prénom', () => {
    creer();
    http.expectOne(`${BASE}/sinistres/declaration`).flush(CONTEXTE);
    rendre();

    expect(page.querySelector('.note-champs')?.textContent).toContain('Champs obligatoires');
    expect(page.querySelector('.note-champs .rouge')?.textContent).toBe('*');
    expect(page.textContent?.replace(/\s+/g, ' ')).toContain('Camille, nous vous guidons');
    expect(tuiles()).toHaveLength(1);
    expect(tuiles()[0].getAttribute('aria-pressed')).toBe('false');
    // Les types n'apparaissent qu'une fois le contrat choisi.
    expect(Array.from(page.querySelectorAll('h2')).some((h2) => h2.textContent?.includes('Que s’est-il passé'))).toBe(
      false,
    );
    expect(bouton('Suivant').disabled).toBe(true);
  });

  it('AC2 : choisir le contrat affiche les types du produit en tuiles, sans présélection, « Autre » en dernier', () => {
    creer();
    http.expectOne(`${BASE}/sinistres/declaration`).flush(CONTEXTE);
    rendre();
    tuiles()[0].click();
    http.expectOne(`${BASE}/contrats/AE-RECETTE-A04/types-sinistre`).flush(TYPES_AUTO);
    rendre();

    const types = tuiles().slice(1);
    expect(types.map((t) => t.querySelector('.tuile__titre')?.textContent)).toEqual([
      'Accident avec un tiers',
      'Vol du véhicule',
      'Autre / je ne sais pas',
    ]);
    expect(types.every((t) => t.getAttribute('aria-pressed') === 'false')).toBe(true);
  });

  it('AC3, AC8 : choisir un type crée le brouillon sans recharger la page et signale la non-couverture', () => {
    creer({}, { contrat: 'AE-RECETTE-A04' });
    http.expectOne(`${BASE}/sinistres/declaration`).flush(CONTEXTE);
    http.expectOne(`${BASE}/contrats/AE-RECETTE-A04/types-sinistre`).flush(TYPES_AUTO);
    rendre();
    // Depuis le raccourci du contrat, ce contrat est celui que le client a choisi.
    expect(tuiles()[0].getAttribute('aria-pressed')).toBe('true');

    tuiles()[2].click();
    const creation = http.expectOne(`${BASE}/sinistres`);
    expect(creation.request.body).toEqual({ numeroContrat: 'AE-RECETTE-A04', type: 'VOL' });
    creation.flush(BROUILLON_VOL);
    rendre();

    expect(TestBed.inject(Location).replaceState).toHaveBeenCalledWith('/espace-client/sinistres/42/type');
    expect(page.textContent).toContain('n’est pas couvert par votre formule Tiers');
    expect(page.textContent).toContain('Vous pouvez quand même le déclarer');
    expect(bouton('Suivant').disabled).toBe(false);
  });

  it('AC10 : numéro d’assistance 24h/24 en haut de l’étape', () => {
    creer();
    http.expectOne(`${BASE}/sinistres/declaration`).flush(CONTEXTE);
    rendre();
    const urgence = page.querySelector('iard-bandeau-urgence a') as HTMLAnchorElement;
    expect(urgence.getAttribute('href')).toBe('tel:0123456789');
  });

  it('AC8 : un brouillon repris garde son contrat ; « Suivant » enregistre puis ouvre les circonstances', () => {
    creer({ id: '42' });
    http.expectOne(`${BASE}/sinistres/42`).flush(BROUILLON_VOL);
    http.expectOne(`${BASE}/contrats/AE-RECETTE-A04/types-sinistre`).flush(TYPES_AUTO);
    rendre();
    expect(tuiles()[2].getAttribute('aria-pressed')).toBe('true');

    bouton('Suivant').click();
    const enregistrement = http.expectOne(`${BASE}/sinistres/42`);
    expect(enregistrement.request.method).toBe('PUT');
    expect(enregistrement.request.body.etape).toBe(2);
    enregistrement.flush(BROUILLON_VOL);
    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/sinistres', '42', 'circonstances'], undefined);
  });

  it('AC8 : « Enregistrer et terminer plus tard » ramène à Mes sinistres avec un message', () => {
    creer({ id: '42' });
    http.expectOne(`${BASE}/sinistres/42`).flush(BROUILLON_VOL);
    http.expectOne(`${BASE}/contrats/AE-RECETTE-A04/types-sinistre`).flush(TYPES_AUTO);
    rendre();

    bouton('Enregistrer et terminer plus tard').click();
    http.expectOne(`${BASE}/sinistres/42`).flush(BROUILLON_VOL);
    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/sinistres'], {
      state: { message: MESSAGE_ENREGISTRE },
    });
  });

  it('AC12 : « Autre » change le type du brouillon et explique que le gestionnaire choisira', () => {
    creer({ id: '42' });
    http.expectOne(`${BASE}/sinistres/42`).flush(BROUILLON_VOL);
    http.expectOne(`${BASE}/contrats/AE-RECETTE-A04/types-sinistre`).flush(TYPES_AUTO);
    rendre();

    tuiles()[3].click();
    const changement = http.expectOne(`${BASE}/sinistres/42`);
    expect(changement.request.body.type).toBe('AUTRE');
    changement.flush({ ...BROUILLON_VOL, type: 'AUTRE', controles: { ...BROUILLON_VOL.controles, couvert: null } });
    rendre();
    expect(page.textContent).toContain('votre gestionnaire choisira le bon type de sinistre');
    expect(page.textContent).not.toContain('n’est pas couvert');
  });

  it('AC1 : sans contrat ouvert à la déclaration, un message renvoie vers l’assistance ; erreur claire sinon', () => {
    creer();
    http.expectOne(`${BASE}/sinistres/declaration`).flush({ ...CONTEXTE, contrats: [] });
    rendre();
    expect(page.textContent).toContain(MESSAGE_AUCUN_CONTRAT);
  });

  it('un brouillon expiré affiche un message compréhensible', () => {
    creer({ id: '42' });
    http.expectOne(`${BASE}/sinistres/42`).flush(null, { status: 404, statusText: 'x' });
    rendre();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Un brouillon est conservé 30 jours');
  });

  it('un refus à la création reste affiché sous les tuiles', () => {
    creer({}, { contrat: 'AE-RECETTE-A04' });
    http.expectOne(`${BASE}/sinistres/declaration`).flush(CONTEXTE);
    http.expectOne(`${BASE}/contrats/AE-RECETTE-A04/types-sinistre`).flush(TYPES_AUTO);
    rendre();
    tuiles()[2].click();
    http.expectOne(`${BASE}/sinistres`).flush({ detail: 'Ce contrat ne permet pas de déclarer.' }, {
      status: 422,
      statusText: 'x',
    });
    rendre();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Ce contrat ne permet pas de déclarer.');
  });
});
