import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DossiersClients } from './dossiers-clients';

describe('DossiersClients (US-04, back-office)', () => {
  let fixture: ComponentFixture<DossiersClients>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DossiersClients);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const repondre = (corps: object, status = 200): void => {
    http.expectOne('/api/assurance-iard-api/back-office/clients').flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };

  it('liste les clients avec un lien vers leur dossier', () => {
    expect(page.textContent).toContain('Chargement des clients');
    repondre([
      { id: 1, prenom: 'Alice', nom: 'Martin', email: 'a@exemple.fr', profil: 'ASSURE' },
      { id: 2, prenom: null, nom: null, email: 'p@exemple.fr', profil: 'PROSPECT' },
    ]);

    const titres = Array.from(page.querySelectorAll('h2')).map((h) => h.textContent);
    expect(titres).toEqual(['Alice Martin', 'p@exemple.fr']);
    expect(Array.from(page.querySelectorAll('.badge')).map((b) => b.textContent)).toEqual(['Assuré', 'Prospect']);
    expect(page.querySelector('.devis-ligne a')?.getAttribute('href')).toBe('/back-office/clients/1');
  });

  it("dit quand il n'y a aucun client", () => {
    repondre([]);
    expect(page.textContent).toContain('Aucun client pour le moment.');
  });

  it('affiche un message clair si le chargement échoue', () => {
    repondre({}, 500);
    expect(page.querySelector('[role="alert"]')).not.toBeNull();
  });
});
