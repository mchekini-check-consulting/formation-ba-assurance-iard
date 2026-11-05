import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DevisService } from '../../core/service/devis.service';
import { DevisContrat } from './devis-contrat';

const UN_CONTRAT = {
  numero: 'AE-2026-000001',
  statut: 'EN_ATTENTE_DE_SIGNATURE',
  civilite: 'M.',
  prenom: 'Jean',
  nom: 'Dupont',
  telephone: '0612345678',
  adresseLogement: '12 rue des Lilas',
  codePostal: '75001',
  ville: 'Paris',
  typeLogement: 'appartement',
  surface: 45,
  formule: 'confort',
  options: ['assistance-depannage'],
  franchise: 300,
  cotisationMensuelle: 18.5,
  cotisationAnnuelle: 222.0,
  jourPrelevement: 10,
  ibanFin: '7890',
  ibanDebut: 'FR76',
  rum: 'AE-2026-ABCDEF12',
  datePremierPrelevement: '2026-12-10',
  dateEffet: '2026-12-01',
  resiliationHamon: false,
  dateCreation: '2026-11-05',
};

const remplirDevis = (devis: DevisService): void => {
  devis.validerRecommandation('7c9e6679-7425-40de-944b-e07fc1f90ae7', 'confort');
  devis.franchiseChoisie.set('300');
  devis.mettreAJour({
    adresse: '12 rue des Lilas',
    codePostal: '75001',
    ville: 'Paris',
    typeLogement: 'appartement',
    surface: '45',
  });
  devis.mandatSigne.set(true);
};

describe('DevisContrat', () => {
  let fixture: ComponentFixture<DevisContrat>;
  let page: HTMLElement;
  let devis: DevisService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    devis = TestBed.inject(DevisService);
    remplirDevis(devis);
    fixture = TestBed.createComponent(DevisContrat);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('appelle POST /devis/contrat au démarrage', () => {
    const requete = http.expectOne((req) => req.method === 'POST' && req.url.includes('/devis/contrat'));
    expect(requete.request.body.referenceDevis).toBe('7c9e6679-7425-40de-944b-e07fc1f90ae7');
    requete.flush(UN_CONTRAT);
  });

  it('affiche le numéro de contrat après génération', () => {
    http.expectOne((req) => req.method === 'POST').flush(UN_CONTRAT);
    fixture.detectChanges();
    expect(page.textContent).toContain('AE-2026-000001');
  });

  it('affiche le nom du souscripteur en titlecase', () => {
    http.expectOne((req) => req.method === 'POST').flush(UN_CONTRAT);
    fixture.detectChanges();
    expect(page.textContent).toContain('Dupont');
  });

  it('affiche un message de chargement initialement', () => {
    expect(page.textContent).toContain('Génération');
    http.expectOne((req) => req.method === 'POST').flush(UN_CONTRAT);
  });

  it('affiche la mention Hamon si le contrat le prévoit', () => {
    http.expectOne((req) => req.method === 'POST').flush({ ...UN_CONTRAT, resiliationHamon: true });
    fixture.detectChanges();
    expect(page.textContent).toContain('Hamon');
  });

  it('affiche le badge signé après une signature réussie', () => {
    http.expectOne((req) => req.method === 'POST').flush(UN_CONTRAT);
    fixture.detectChanges();

    const checkbox = page.querySelector<HTMLInputElement>('input[type="checkbox"]')!;
    checkbox.checked = true;
    checkbox.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const codeInput = page.querySelector<HTMLInputElement>('#code-signature')!;
    codeInput.value = '123456';
    codeInput.dispatchEvent(new InputEvent('input'));
    fixture.detectChanges();

    const bouton = page.querySelector<HTMLButtonElement>('.btn')!;
    bouton.click();
    fixture.detectChanges();

    http.expectOne((req) => req.method === 'POST' && req.url.includes('/signature'))
      .flush({ ...UN_CONTRAT, statut: 'SIGNE' });
    fixture.detectChanges();

    expect(page.textContent).toContain('Signé');
  });
});
