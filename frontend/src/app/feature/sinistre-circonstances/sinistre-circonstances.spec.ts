import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { DeclarationSinistre, MESSAGE_BLESSES, MESSAGE_HORS_PERIODE } from '../../core/model/sinistre';
import { BROUILLON_COMPLET, BROUILLON_VOL } from '../../core/model/sinistre.exemples';
import { SinistreCirconstances } from './sinistre-circonstances';

const ADRESSE = '/api/assurance-iard-api/sinistres/42';
const EN_RETARD: DeclarationSinistre = {
  ...BROUILLON_COMPLET,
  controles: { ...BROUILLON_COMPLET.controles, horsDelai: true, horsPeriodeContrat: true },
};

describe('SinistreCirconstances, étape 2 (US-24, AC3 à AC6, AC11 à AC13)', () => {
  let fixture: ComponentFixture<SinistreCirconstances>;
  let page: HTMLElement;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '42' })) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(SinistreCirconstances);
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const charger = (declaration: DeclarationSinistre = BROUILLON_VOL): void => {
    http.expectOne(ADRESSE).flush(declaration);
    fixture.detectChanges();
  };
  const champ = <T extends HTMLElement>(selecteur: string): T => page.querySelector<T>(selecteur)!;
  const saisir = (selecteur: string, valeur: string, evenement = 'input'): void => {
    const element = champ<HTMLInputElement>(selecteur);
    element.value = valeur;
    element.dispatchEvent(new Event(evenement));
    fixture.detectChanges();
  };
  const cocher = (nom: string, libelle: string): void => {
    const radio = Array.from(page.querySelectorAll<HTMLInputElement>(`input[name="${nom}"]`)).find((r) =>
      r.parentElement?.textContent?.includes(libelle),
    )!;
    radio.checked = true;
    radio.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const bouton = (texte: string): HTMLButtonElement =>
    Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes(texte))!;
  const remplir = (): void => {
    saisir('#date-sinistre', '2026-11-25', 'change');
    saisir('#lieu-sinistre', 'Parking du travail');
    saisir('#description-sinistre', 'Véhicule disparu le matin');
    cocher('tiers', 'Non');
    cocher('blesses', 'Non');
    cocher('declarant', 'Moi-même');
  };

  it('AC4 : aucune réponse présélectionnée ; questions propres au vol ; date limitée à aujourd’hui', () => {
    charger();
    expect(Array.from(page.querySelectorAll<HTMLInputElement>('input[type="radio"]')).some((r) => r.checked)).toBe(
      false,
    );
    expect(page.textContent).toContain('Avez-vous déposé plainte ?');
    expect(page.textContent).toContain('Le véhicule a-t-il été retrouvé ?');
    expect(champ<HTMLInputElement>('#date-sinistre').max).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(page.querySelectorAll('label .rouge').length).toBeGreaterThanOrEqual(3);
  });

  it('AC4 : « Suivant » signale chaque réponse obligatoire manquante avec un message clair, sans appel', () => {
    charger();
    bouton('Suivant').click();
    fixture.detectChanges();
    expect(page.textContent).toContain('Indiquez la date du sinistre.');
    expect(page.textContent).toContain('Indiquez le lieu du sinistre.');
    expect(page.textContent).toContain('Indiquez si un tiers est impliqué.');
    expect(page.textContent).toContain('Indiquez qui déclare le sinistre.');
    expect(page.textContent).not.toMatch(/invalide/i);
  });

  it('AC4 : une date dans le futur est refusée', () => {
    charger();
    remplir();
    saisir('#date-sinistre', '2999-01-01', 'change');
    bouton('Suivant').click();
    fixture.detectChanges();
    expect(page.querySelector('#erreur-date')?.textContent).toBe('La date du sinistre ne peut pas être dans le futur.');
  });

  it('AC11 : blessés oui → message et nombre obligatoire, sans aucun champ médical', () => {
    charger();
    cocher('blesses', 'Oui');
    expect(page.textContent).toContain(MESSAGE_BLESSES);
    expect(champ('#nombre-blesses')).not.toBeNull();
    expect(page.textContent).not.toMatch(/nature des blessures|médecin|hôpital/i);
  });

  it('AC13 : un proche donne son nom et son lien ; l’assuré sera informé', () => {
    charger();
    cocher('declarant', 'Un proche');
    expect(champ('#declarant-nom')).not.toBeNull();
    expect(champ('#declarant-lien')).not.toBeNull();
    expect(page.textContent).toContain('L’assuré sera informé de cette déclaration par e-mail');
    // Auto : le conducteur secondaire est proposé.
    expect(page.textContent).toContain('Le conducteur secondaire déclaré');
  });

  it('AC5, AC6, AC3 : messages après « Suivant », explication obligatoire, puis on peut continuer', () => {
    charger();
    remplir();
    bouton('Suivant').click();
    const premier = http.expectOne(ADRESSE);
    expect(premier.request.method).toBe('PUT');
    expect(premier.request.body).toMatchObject({ etape: 2, dateSurvenance: '2026-11-25', declarant: 'SOUSCRIPTEUR' });
    premier.flush(EN_RETARD);
    fixture.detectChanges();

    expect(page.textContent).toContain(MESSAGE_HORS_PERIODE);
    expect(page.textContent).toContain('Le délai prévu pour déclarer ce type de sinistre est de 2 jours ouvrés');
    expect(page.textContent).toContain('n’est pas couvert par votre formule Tiers');
    expect(router.navigate).not.toHaveBeenCalled();

    bouton('Suivant').click();
    fixture.detectChanges();
    expect(page.textContent).toContain('Expliquez en quelques mots la raison du délai de déclaration.');

    saisir('#explication-retard', 'J’étais à l’étranger');
    bouton('Suivant').click();
    http.expectOne(ADRESSE).flush({ ...EN_RETARD, explicationRetard: 'J’étais à l’étranger' });
    const etape3 = http.expectOne(ADRESSE);
    expect(etape3.request.body.etape).toBe(3);
    etape3.flush(EN_RETARD);
    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/sinistres', '42', 'justificatifs']);
  });

  it('sans message à montrer, « Suivant » enregistre et ouvre l’étape 3', () => {
    charger();
    remplir();
    cocher('question-0', 'Oui');
    bouton('Suivant').click();
    const premier = http.expectOne(ADRESSE);
    expect(premier.request.body.details).toEqual({ 'Avez-vous déposé plainte ?': 'Oui' });
    premier.flush({ ...BROUILLON_COMPLET, controles: { ...BROUILLON_COMPLET.controles, couvert: true } });
    http.expectOne(ADRESSE).flush(BROUILLON_COMPLET);
    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/sinistres', '42', 'justificatifs']);
  });

  it('AC8 : « Enregistrer et terminer plus tard » garde des réponses incomplètes', () => {
    charger();
    saisir('#lieu-sinistre', 'Parking');
    bouton('Enregistrer et terminer plus tard').click();
    const enregistrement = http.expectOne(ADRESSE);
    expect(enregistrement.request.body).toMatchObject({ lieu: 'Parking', dateSurvenance: null, etape: 2 });
    enregistrement.flush(BROUILLON_VOL);
    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/sinistres'], expect.anything());
  });

  it('« Précédent » enregistre et revient à l’étape 1 ; un refus du serveur reste lisible', () => {
    charger();
    bouton('Précédent').click();
    http.expectOne(ADRESSE).flush({ detail: 'La date du sinistre ne peut pas être dans le futur.' }, {
      status: 400,
      statusText: 'x',
    });
    fixture.detectChanges();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('ne peut pas être dans le futur');

    bouton('Précédent').click();
    http.expectOne(ADRESSE).flush(BROUILLON_VOL);
    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/sinistres', '42', 'type']);
  });

  it('un brouillon repris affiche tout de suite ses messages ; tiers et catastrophe naturelle', () => {
    charger({
      ...EN_RETARD,
      type: 'CLIMATIQUE_CATNAT',
      tiersImplique: true,
      tiers: { nom: 'M. Dupont', immatriculation: null, assureur: null },
    });
    expect(page.textContent).toContain(MESSAGE_HORS_PERIODE);
    expect(champ('#date-arrete')).not.toBeNull();
    expect(champ<HTMLInputElement>('#tiers-nom').value).toBe('M. Dupont');
    saisir('#tiers-assureur', 'Assureur X');
    saisir('#date-arrete', '2026-11-20', 'change');
    // Changer une date fait recalculer les messages au prochain « Suivant ».
    expect(page.textContent).not.toContain(MESSAGE_HORS_PERIODE);
  });
});
