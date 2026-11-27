import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { BROUILLON_COMPLET } from '../../core/model/sinistre.exemples';
import { PROCHAINES_ETAPES, SinistreEnvoyePage } from './sinistre-envoye';

const ADRESSE = '/api/assurance-iard-api/sinistres/42';
const ENVOYE = {
  ...BROUILLON_COMPLET,
  statut: 'DECLARE' as const,
  numero: 'SIN-2026-000042',
  declareLe: '2026-11-26T10:00:00Z',
};

describe('SinistreEnvoyePage, « Déclaration envoyée » (US-24, AC9)', () => {
  let fixture: ComponentFixture<SinistreEnvoyePage>;
  let page: HTMLElement;
  let http: HttpTestingController;

  const creer = (etat?: object): void => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '42' })) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    if (etat) {
      vi.spyOn(TestBed.inject(Router), 'currentNavigation').mockReturnValue({ extras: { state: etat } } as never);
    }
    fixture = TestBed.createComponent(SinistreEnvoyePage);
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  };

  afterEach(() => http.verify());

  it('AC9 : numéro de dossier, statut Déclaré, prénom, prochaines étapes et accusé de réception', () => {
    creer({ envoi: { emailEnvoye: true } });
    http.expectOne(ADRESSE).flush(ENVOYE);
    fixture.detectChanges();

    expect(page.querySelector('h1')?.textContent).toContain('Merci Camille');
    expect(page.querySelector('[role="status"]')?.textContent).toContain('Dossier SIN-2026-000042 · Déclaré');
    expect(page.textContent).toContain('déclaré le 26/11/2026');
    expect(Array.from(page.querySelectorAll('ol li')).map((li) => li.textContent)).toEqual(PROCHAINES_ETAPES);
    expect(page.textContent).toContain('Un accusé de réception vous est envoyé par e-mail.');
    // AC7 : pièces obligatoires manquantes.
    expect(page.textContent).toContain('Compléter mes justificatifs');
  });

  it('si l’e-mail n’est pas parti, le client sait que son dossier est bien enregistré', () => {
    creer({ envoi: { emailEnvoye: false } });
    http.expectOne(ADRESSE).flush({ ...ENVOYE, piecesManquantes: false });
    fixture.detectChanges();
    expect(page.textContent).toContain('n’a pas pu vous être envoyé par e-mail');
    expect(page.textContent).not.toContain('Compléter mes justificatifs');
  });

  it('erreur compréhensible si le dossier est introuvable', () => {
    creer();
    http.expectOne(ADRESSE).flush(null, { status: 404, statusText: 'x' });
    fixture.detectChanges();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('introuvable');
  });
});
