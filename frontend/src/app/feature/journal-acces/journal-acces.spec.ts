import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { JournalAcces } from './journal-acces';

describe('JournalAcces (US-04, RG3)', () => {
  let fixture: ComponentFixture<JournalAcces>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(JournalAcces);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const repondre = (corps: object, status = 200): void => {
    http.expectOne('/api/assurance-iard-api/admin/acces-refuses').flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };

  it('affiche date, utilisateur, ressource demandée, action et motif', () => {
    expect(page.textContent).toContain('Chargement du journal');
    repondre([
      {
        date: '2026-11-11T10:00:00Z',
        email: 'a@exemple.fr',
        profil: 'ASSURE',
        methode: 'GET',
        chemin: '/documents/9',
        ressource: 'DOCUMENT',
        identifiant: '9',
        rubrique: 'DOCUMENTS',
        droit: 'CONSULTER',
        motif: 'PAS_PROPRIETAIRE',
      },
      { date: '2026-11-11T09:00:00Z', email: 'b@exemple.fr', profil: 'ASSURE', methode: 'GET', chemin: '/x', ressource: 'AUCUNE', identifiant: null, rubrique: 'PROFILS', droit: 'CONSULTER', motif: 'INCONNU' },
    ]);

    const cellules = Array.from(page.querySelectorAll('tbody tr:first-child td')).map((td) => td.textContent?.trim());
    expect(cellules[0]).toContain('11/11/2026');
    expect(cellules[1]).toContain('a@exemple.fr');
    expect(cellules[2]).toBe('GET /documents/9');
    expect(cellules[3]).toBe('DOCUMENTS / CONSULTER');
    expect(cellules[4]).toBe("Donnée d'un autre client");
    expect(page.querySelector('tbody tr:last-child td:last-child')?.textContent).toBe('INCONNU');
  });

  it('dit quand aucun accès n’a été refusé', () => {
    repondre([]);
    expect(page.textContent).toContain('Aucun accès refusé pour le moment.');
  });

  it('affiche un message clair si le chargement échoue', () => {
    repondre({}, 500);
    expect(page.querySelector('[role="alert"]')).not.toBeNull();
  });
});
