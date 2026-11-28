import { registerLocaleData } from '@angular/common';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import localeFr from '@angular/common/locales/fr';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { DevisAutoContrat } from './devis-auto-contrat';
import { ContratAutoDetail } from '../../core/model/devis-auto';

registerLocaleData(localeFr);

const CONTRAT_FIXTURE: ContratAutoDetail = {
  numero: 'AUTO-2024-00042',
  statut: 'EN_ATTENTE_DE_SIGNATURE',
  civilite: 'M.',
  prenom: 'Jean',
  nom: 'Dupont',
  vehiculeMarque: 'Renault',
  vehiculeModele: 'Clio',
  vehiculeVersion: '1.0 TCe 90',
  vehiculeImmatriculation: 'AB-123-CD',
  coefficientBonusMalus: 0.85,
  nomConducteur: 'Jean Dupont',
  formule: 'TIERS',
  options: ['ASSISTANCE_0KM'],
  franchise: 300,
  cotisationMensuelle: 42.5,
  cotisationAnnuelle: 510,
  dateEffet: '2024-02-01',
  jourPrelevement: 5,
  ibanMasque: 'FR76 **** **** **** **** 1234',
  rum: 'RUM-TEST-001',
  ancienAssureur: null,
  ancienNumeroContrat: null,
  conditionsParticulieresId: 10,
  memoVehiculeId: 11,
  mandatSepaId: 12,
  conditionsGeneralesId: 20,
  ipidId: 21,
  ficheConseilId: 22,
  justificatifsAttendus: ['Permis de conduire du conducteur principal', 'Carte grise du véhicule assuré'],
  exclusions: ['Vol sans effraction', 'Dommages en compétition'],
};

describe('DevisAutoContrat', () => {
  let http: HttpTestingController;
  let devisAutoSvc: DevisAutoService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DevisAutoContrat],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'fr-FR' },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    devisAutoSvc = TestBed.inject(DevisAutoService);
    devisAutoSvc.enregistrerDevisAutoId(99);
  });

  afterEach(() => http.verify());

  it('affiche l\'étape de chargement puis les données du contrat', async () => {
    const fixture = TestBed.createComponent(DevisAutoContrat);
    fixture.detectChanges();

    expect(fixture.componentInstance['etape']()).toBe('chargement');

    http.expectOne((r) => r.url.includes('/v1/devis/auto/99/contrat/signature')).flush(CONTRAT_FIXTURE);
    fixture.detectChanges();

    expect(fixture.componentInstance['etape']()).toBe('lecture');
    expect(fixture.componentInstance['contrat']()?.numero).toBe('AUTO-2024-00042');
  });

  it('passe en erreur-chargement si le GET échoue', async () => {
    const fixture = TestBed.createComponent(DevisAutoContrat);
    fixture.detectChanges();

    http.expectOne((r) => r.url.includes('/signature')).flush('', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance['etape']()).toBe('erreur-chargement');
  });

  it('envoie le code et passe en code-envoye', () => {
    const fixture = TestBed.createComponent(DevisAutoContrat);
    fixture.componentInstance['contrat'].set(CONTRAT_FIXTURE);
    fixture.componentInstance['etape'].set('lecture');
    fixture.componentInstance['cgAcceptees'].set(true);
    fixture.detectChanges();

    http.expectOne((r) => r.url.includes('/signature')).flush(CONTRAT_FIXTURE);

    fixture.componentInstance['envoyerCode']();
    http.expectOne((r) => r.url.includes('/signature/code') && !r.url.includes('renvoyer'))
      .flush({ emailMasque: 'j***@gmail.com' });
    fixture.detectChanges();

    expect(fixture.componentInstance['etape']()).toBe('code-envoye');
    expect(fixture.componentInstance['emailMasque']()).toBe('j***@gmail.com');
  });

  it('affiche un message si le code est incorrect (422)', () => {
    const fixture = TestBed.createComponent(DevisAutoContrat);
    fixture.componentInstance['contrat'].set(CONTRAT_FIXTURE);
    fixture.componentInstance['etape'].set('code-envoye');
    fixture.componentInstance['code'].set('999999');
    fixture.detectChanges();

    http.expectOne((r) => r.url.includes('/signature')).flush(CONTRAT_FIXTURE);

    fixture.componentInstance['signer']();
    http.expectOne((r) => r.url.endsWith('/signature') && r.method === 'POST')
      .flush({ essaisRestants: 2 }, { status: 422, statusText: 'Unprocessable Entity' });
    fixture.detectChanges();

    expect(fixture.componentInstance['messageErreur']()).toContain('2 tentatives');
  });

  it('passe en signe après une signature réussie', () => {
    const signedContrat = { ...CONTRAT_FIXTURE, statut: 'ACTIF_JUSTIFICATIFS_ATTENDUS' };
    const fixture = TestBed.createComponent(DevisAutoContrat);
    fixture.componentInstance['contrat'].set(CONTRAT_FIXTURE);
    fixture.componentInstance['etape'].set('code-envoye');
    fixture.componentInstance['code'].set('123456');
    fixture.detectChanges();

    http.expectOne((r) => r.url.includes('/signature')).flush(CONTRAT_FIXTURE);

    fixture.componentInstance['signer']();
    http.expectOne((r) => r.url.endsWith('/signature') && r.method === 'POST').flush(signedContrat);
    fixture.detectChanges();

    expect(fixture.componentInstance['etape']()).toBe('signe');
    expect(fixture.componentInstance['contratSigne']()?.statut).toBe('ACTIF_JUSTIFICATIFS_ATTENDUS');
  });

  it('affiche un message si les essais sont épuisés (409)', () => {
    const fixture = TestBed.createComponent(DevisAutoContrat);
    fixture.componentInstance['etape'].set('code-envoye');
    fixture.componentInstance['code'].set('000000');
    fixture.detectChanges();

    http.expectOne((r) => r.url.includes('/signature')).flush(CONTRAT_FIXTURE);

    fixture.componentInstance['signer']();
    http.expectOne((r) => r.url.endsWith('/signature') && r.method === 'POST')
      .flush({}, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(fixture.componentInstance['messageErreur']()).toContain('épuisé');
  });

  it('renvoie le code, réinitialise la saisie et met à jour l\'email masqué', () => {
    const fixture = TestBed.createComponent(DevisAutoContrat);
    fixture.componentInstance['etape'].set('code-envoye');
    fixture.componentInstance['code'].set('123456');
    fixture.detectChanges();

    http.expectOne((r) => r.url.includes('/signature')).flush(CONTRAT_FIXTURE);

    fixture.componentInstance['renvoyerCode']();
    http.expectOne((r) => r.url.includes('/signature/code/renvoyer'))
      .flush({ emailMasque: 'n***@outlook.fr' });
    fixture.detectChanges();

    expect(fixture.componentInstance['code']()).toBe('');
    expect(fixture.componentInstance['emailMasque']()).toBe('n***@outlook.fr');
    expect(fixture.componentInstance['messageErreur']()).toBeNull();
  });

  it('passe directement à la confirmation si le contrat est déjà signé au chargement', async () => {
    const fixture = TestBed.createComponent(DevisAutoContrat);
    fixture.detectChanges();

    const contratActif = { ...CONTRAT_FIXTURE, statut: 'ACTIF_JUSTIFICATIFS_ATTENDUS' };
    http.expectOne((r) => r.url.includes('/signature')).flush(contratActif);
    fixture.detectChanges();

    expect(fixture.componentInstance['etape']()).toBe('signe');
    expect(fixture.componentInstance['contratSigne']()?.statut).toBe('ACTIF_JUSTIFICATIFS_ATTENDUS');
  });
});
