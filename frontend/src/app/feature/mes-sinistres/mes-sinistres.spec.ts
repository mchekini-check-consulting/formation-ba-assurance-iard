import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SinistreResume } from '../../core/model/sinistre';
import { MesSinistres } from './mes-sinistres';

const URL_LISTE = '/api/assurance-iard-api/sinistres';
const LISTE: SinistreResume[] = [
  {
    id: 2,
    numero: null,
    statut: 'BROUILLON',
    type: 'ACCIDENT_SANS_TIERS',
    libelleType: 'Accident sans tiers',
    numeroContrat: 'AE-RECETTE-A03',
    produit: 'AUTO',
    dateSurvenance: '2026-11-24',
    etape: 2,
    piecesManquantes: false,
    declareLe: null,
    brouillonExpireLe: '2026-12-20T10:00:00Z',
  },
  {
    id: 1,
    numero: 'SIN-2026-000001',
    statut: 'DECLARE',
    type: 'DEGAT_EAUX',
    libelleType: 'Dégât des eaux',
    numeroContrat: 'AE-RECETTE-H01',
    produit: 'HABITATION',
    dateSurvenance: '2026-11-15',
    etape: 3,
    piecesManquantes: true,
    declareLe: '2026-11-16T09:00:00Z',
    brouillonExpireLe: null,
  },
];

describe('MesSinistres (US-24, AC7, AC8)', () => {
  let fixture: ComponentFixture<MesSinistres>;
  let page: HTMLElement;
  let http: HttpTestingController;

  const creer = (etat?: object): void => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    if (etat) {
      vi.spyOn(TestBed.inject(Router), 'currentNavigation').mockReturnValue({ extras: { state: etat } } as never);
    }
    fixture = TestBed.createComponent(MesSinistres);
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  };

  afterEach(() => http.verify());

  it('AC8 : brouillons à reprendre (30 jours) et dossiers déclarés, dates au format JJ/MM/AAAA', () => {
    creer();
    http.expectOne(URL_LISTE).flush(LISTE);
    fixture.detectChanges();

    const lignes = Array.from(page.querySelectorAll('.devis-ligne'));
    expect(lignes[0].textContent).toContain('Brouillon');
    expect(lignes[0].textContent).toContain('survenu le 24/11/2026');
    expect(lignes[0].textContent).toContain('conservé jusqu’au 20/12/2026');
    expect(lignes[0].querySelector('a')?.getAttribute('href')).toBe('/espace-client/sinistres/2/circonstances');
    expect(lignes[1].textContent).toContain('Dossier SIN-2026-000001');
    expect(lignes[1].textContent).toContain('Déclaré le 16/11/2026');
    // AC7 : un dossier dont des pièces obligatoires manquent peut être complété.
    expect(lignes[1].querySelector('a')?.getAttribute('href')).toBe('/espace-client/sinistres/1/justificatifs');
    expect(page.querySelector('a.btn')?.getAttribute('href')).toBe('/espace-client/sinistres/declarer');
  });

  it('affiche le message de l’étape quittée et une liste vide', () => {
    creer({ message: 'Votre brouillon est enregistré.' });
    http.expectOne(URL_LISTE).flush([]);
    fixture.detectChanges();
    expect(page.querySelector('[role="status"]')?.textContent).toContain('Votre brouillon est enregistré.');
    expect(page.textContent).toContain('Vous n’avez déclaré aucun sinistre.');
  });

  it('une erreur est expliquée simplement', () => {
    creer();
    http.expectOne(URL_LISTE).flush(null, { status: 500, statusText: 'x' });
    fixture.detectChanges();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Réessayez dans quelques instants');
  });
});
