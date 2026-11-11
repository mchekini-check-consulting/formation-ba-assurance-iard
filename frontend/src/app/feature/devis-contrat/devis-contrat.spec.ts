import { RECO_CONFORT } from '../../core/model/tarif.exemples';
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
  rum: 'AE-2026-ABCDEF12',
  datePremierPrelevement: '2026-12-10',
  dateEffet: '2026-12-01',
  resiliationHamon: false,
  dateCreation: '2026-11-05',
};

const remplirDevis = (devis: DevisService): void => {
  devis.validerRecommandation('7c9e6679-7425-40de-944b-e07fc1f90ae7', RECO_CONFORT);
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

const genererContrat = (fixture: ComponentFixture<DevisContrat>, http: HttpTestingController): void => {
  http.expectOne((req) => req.method === 'POST' && req.url.includes('/devis/contrat') && !req.url.includes('/signature')).flush(UN_CONTRAT);
  fixture.detectChanges();
};

const cocherAcceptation = (page: HTMLElement, fixture: ComponentFixture<DevisContrat>): void => {
  const checkbox = page.querySelector<HTMLInputElement>('input[type="checkbox"]')!;
  checkbox.checked = true;
  checkbox.dispatchEvent(new Event('change'));
  fixture.detectChanges();
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

  it('appelle POST /devis/contrat au demarrage', () => {
    const requete = http.expectOne((req) => req.method === 'POST' && req.url.includes('/devis/contrat'));
    expect(requete.request.body.referenceDevis).toBe('7c9e6679-7425-40de-944b-e07fc1f90ae7');
    requete.flush(UN_CONTRAT);
  });

  it('affiche le numero de contrat apres generation', () => {
    genererContrat(fixture, http);
    expect(page.textContent).toContain('AE-2026-000001');
  });

  it('affiche le nom du souscripteur en titlecase', () => {
    genererContrat(fixture, http);
    expect(page.textContent).toContain('Dupont');
  });

  it('affiche un message de chargement initialement', () => {
    expect(page.textContent).toContain('Génération');
    http.expectOne((req) => req.method === 'POST').flush(UN_CONTRAT);
  });

  it('affiche la mention Hamon si le contrat le prevoit', () => {
    http.expectOne((req) => req.method === 'POST').flush({ ...UN_CONTRAT, resiliationHamon: true });
    fixture.detectChanges();
    expect(page.textContent).toContain('Hamon');
  });

  it('AC1 : le bouton Recevoir le code est desactive tant que la case nest pas cochee', () => {
    genererContrat(fixture, http);
    const bouton = page.querySelector<HTMLButtonElement>('button.btn')!;
    expect(bouton.disabled).toBe(true);
  });

  it('AC1 : le bouton Recevoir le code sactive apres avoir coche la case', () => {
    genererContrat(fixture, http);
    cocherAcceptation(page, fixture);
    const bouton = page.querySelector<HTMLButtonElement>('button.btn')!;
    expect(bouton.disabled).toBe(false);
  });

  it('AC2 : envoie le code et affiche email masque', () => {
    genererContrat(fixture, http);
    cocherAcceptation(page, fixture);

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    fixture.detectChanges();

    http.expectOne((req) => req.method === 'POST' && req.url.includes('/signature/code') && !req.url.includes('/renvoyer'))
      .flush({ emailMasque: 'j***@example.com' });
    fixture.detectChanges();

    expect(page.textContent).toContain('j***@example.com');
    expect(page.querySelector('#code-signature')).toBeTruthy();
  });

  it('AC3 : valide la signature et affiche le contrat actif', () => {
    genererContrat(fixture, http);
    cocherAcceptation(page, fixture);

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    http.expectOne((req) => req.url.includes('/signature/code') && !req.url.includes('/renvoyer'))
      .flush({ emailMasque: 'j***@example.com' });
    fixture.detectChanges();

    const codeInput = page.querySelector<HTMLInputElement>('#code-signature')!;
    codeInput.value = '123456';
    codeInput.dispatchEvent(new InputEvent('input'));
    fixture.detectChanges();

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    fixture.detectChanges();

    http.expectOne((req) => req.method === 'POST' && req.url.includes('/signature') && !req.url.includes('/code'))
      .flush({ ...UN_CONTRAT, statut: 'ACTIF' });
    fixture.detectChanges();

    expect(page.textContent).toContain('actif');
  });

  it('AC4 : affiche les essais restants si le code est incorrect', () => {
    genererContrat(fixture, http);
    cocherAcceptation(page, fixture);

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    http.expectOne((req) => req.url.includes('/signature/code') && !req.url.includes('/renvoyer'))
      .flush({ emailMasque: 'j***@example.com' });
    fixture.detectChanges();

    const codeInput = page.querySelector<HTMLInputElement>('#code-signature')!;
    codeInput.value = '999999';
    codeInput.dispatchEvent(new InputEvent('input'));
    fixture.detectChanges();

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    fixture.detectChanges();

    http.expectOne((req) => req.url.includes('/signature') && !req.url.includes('/code'))
      .flush({ title: 'err', essaisRestants: 2 }, { status: 422, statusText: 'Unprocessable Entity' });
    fixture.detectChanges();

    expect(page.textContent).toContain('2 essai');
    expect(page.querySelector('#code-signature')).toBeTruthy();
  });

  it('AC5 : desactive la saisie apres epuisement des essais', () => {
    genererContrat(fixture, http);
    cocherAcceptation(page, fixture);

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    http.expectOne((req) => req.url.includes('/signature/code') && !req.url.includes('/renvoyer'))
      .flush({ emailMasque: 'j***@example.com' });
    fixture.detectChanges();

    const codeInput = page.querySelector<HTMLInputElement>('#code-signature')!;
    codeInput.value = '999999';
    codeInput.dispatchEvent(new InputEvent('input'));
    fixture.detectChanges();

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    fixture.detectChanges();

    http.expectOne((req) => req.url.includes('/signature') && !req.url.includes('/code'))
      .flush({ title: 'err' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(page.querySelector<HTMLInputElement>('#code-signature')!.disabled).toBe(true);
    expect(page.querySelector('button.lien-renvoyer')).toBeTruthy();
  });

  it('AC6 : affiche un message expiration et invite a renvoyer le code', () => {
    genererContrat(fixture, http);
    cocherAcceptation(page, fixture);

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    http.expectOne((req) => req.url.includes('/signature/code') && !req.url.includes('/renvoyer'))
      .flush({ emailMasque: 'j***@example.com' });
    fixture.detectChanges();

    const codeInput = page.querySelector<HTMLInputElement>('#code-signature')!;
    codeInput.value = '123456';
    codeInput.dispatchEvent(new InputEvent('input'));
    fixture.detectChanges();

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    fixture.detectChanges();

    http.expectOne((req) => req.url.includes('/signature') && !req.url.includes('/code'))
      .flush({ title: 'err' }, { status: 410, statusText: 'Gone' });
    fixture.detectChanges();

    expect(page.textContent).toContain('expiré');
    expect(page.querySelector('button.lien-renvoyer')).toBeTruthy();
  });

  it('AC7 : renvoyer le code reinitialise le formulaire', () => {
    genererContrat(fixture, http);
    cocherAcceptation(page, fixture);

    page.querySelector<HTMLButtonElement>('button.btn')!.click();
    http.expectOne((req) => req.url.includes('/signature/code') && !req.url.includes('/renvoyer'))
      .flush({ emailMasque: 'j***@example.com' });
    fixture.detectChanges();

    page.querySelector<HTMLButtonElement>('button.lien-renvoyer')!.click();
    fixture.detectChanges();

    http.expectOne((req) => req.url.includes('/signature/code/renvoyer'))
      .flush({ emailMasque: 'j***@example.com' });
    fixture.detectChanges();

    expect(page.querySelector<HTMLInputElement>('#code-signature')!.value).toBe('');
    expect(page.querySelector<HTMLInputElement>('#code-signature')!.disabled).toBe(false);
  });
});
