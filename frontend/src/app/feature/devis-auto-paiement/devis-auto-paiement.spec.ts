import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { DevisAutoPaiement } from './devis-auto-paiement';
import { DevisAutoService } from '../../core/service/devis-auto.service';

const INIT_AVEC_OPTION = {
  souscripteurPrenom: 'Marie', souscripteurNom: 'Durand',
  souscripteurAdresse: '12 rue de la Paix', souscripteurCodePostal: '75001', souscripteurVille: 'Paris',
  souscripteurEmail: 'marie@test.fr',
  vehiculeMarque: 'Renault', vehiculeModele: 'Clio', vehiculePlaque: 'AA-123-BB',
  formule: 'TIERS', options: [{ code: 'ASSISTANCE_0KM', prixMensuelTtc: 3.50 }],
  franchise: null, prixMensuelTtc: 22.49, prixAnnuelTtc: 269.88,
  hamonApplicable: false, dateFinAncienContrat: null,
  ics: 'FR00ZZZ000000', creancier: "Assur'Easy",
  titulairePrerempli: 'Marie Durand', numeroContratExistant: null, documentMandatSepaIdExistant: null,
  conducteurAnneePermis: 2007, conducteurBonusMalus: 0.68,
};

const INIT_REPONSE = {
  souscripteurPrenom: 'Marie', souscripteurNom: 'Durand',
  souscripteurAdresse: '12 rue de la Paix', souscripteurCodePostal: '75001', souscripteurVille: 'Paris',
  souscripteurEmail: 'marie@test.fr',
  vehiculeMarque: 'Renault', vehiculeModele: 'Clio', vehiculePlaque: 'AA-123-BB',
  formule: 'TR', options: [], franchise: 500, prixMensuelTtc: 55, prixAnnuelTtc: 660,
  hamonApplicable: false, dateFinAncienContrat: null,
  ics: 'FR00ZZZ000000', creancier: "Assur'Easy",
  titulairePrerempli: 'Marie Durand', numeroContratExistant: null, documentMandatSepaIdExistant: null,
  conducteurAnneePermis: 2010, conducteurBonusMalus: 0.68,
};

describe('DevisAutoPaiement', () => {
  let fixture: ComponentFixture<DevisAutoPaiement>;
  let svc: DevisAutoService;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DevisAutoPaiement],
      providers: [
        provideRouter([
          { path: 'connexion', redirectTo: '' },
          { path: 'devis-auto/contrat', redirectTo: '' },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    svc = TestBed.inject(DevisAutoService);
    svc.devisAutoId.set(42);
    svc.souscripteurEnregistre.set(true);

    fixture = TestBed.createComponent(DevisAutoPaiement);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function charger(reponse: object = INIT_REPONSE): void {
    fixture.detectChanges();
    const req = http.expectOne((r) => r.url.includes('/paiement'));
    req.flush(reponse);
    fixture.detectChanges();
  }

  it('affiche le sous-titre et la mention champs obligatoires', () => {
    charger();
    const texte: string = fixture.nativeElement.textContent;
    expect(texte).toContain('Votre cotisation est prélevée chaque mois.');
    expect(texte).toContain('Champs obligatoires');
  });

  it('pré-remplit le titulaire depuis la réponse', () => {
    charger();
    const titulaire = fixture.nativeElement.querySelector('#titulaire') as HTMLInputElement;
    expect(titulaire.value).toBe('Marie Durand');
  });

  it('affiche l\'encadré Hamon bordeaux si hamonApplicable', () => {
    charger({ ...INIT_REPONSE, hamonApplicable: true, dateFinAncienContrat: '2026-12-31' });
    const hamon = fixture.nativeElement.querySelector('.encadre-hamon');
    expect(hamon).toBeTruthy();
    expect(hamon.textContent).toContain('Hamon');
  });

  it('n\'affiche pas l\'encadré Hamon si non applicable', () => {
    charger();
    expect(fixture.nativeElement.querySelector('.encadre-hamon')).toBeNull();
  });

  it('pré-remplit la date de début au lendemain si Hamon applicable', () => {
    charger({ ...INIT_REPONSE, hamonApplicable: true, dateFinAncienContrat: '2026-12-31' });
    const dateInput = fixture.nativeElement.querySelector('#date-debut') as HTMLInputElement;
    expect(dateInput.value).toBe('2027-01-01');
  });

  it('aucun jour n\'est présélectionné au chargement', () => {
    charger();
    const actifs = fixture.nativeElement.querySelectorAll('.choix__option--actif');
    expect(actifs.length).toBe(0);
  });

  it('la case autorisation n\'est pas pré-cochée', () => {
    charger();
    const cas = fixture.nativeElement.querySelector('#autorisation') as HTMLInputElement;
    expect(cas.checked).toBe(false);
  });

  it('le bouton générer est désactivé sans autorisation', () => {
    charger();
    const bouton = fixture.nativeElement.querySelector('[type="submit"]') as HTMLButtonElement;
    expect(bouton.disabled).toBe(true);
  });

  it('le bouton générer est actif après avoir coché l\'autorisation', () => {
    charger();
    const cas = fixture.nativeElement.querySelector('#autorisation') as HTMLInputElement;
    cas.click();
    fixture.detectChanges();
    const bouton = fixture.nativeElement.querySelector('[type="submit"]') as HTMLButtonElement;
    expect(bouton.disabled).toBe(false);
  });

  it('affiche le retour avec le bon libellé', () => {
    charger();
    const retour = fixture.nativeElement.querySelector('.lien-retour') as HTMLAnchorElement;
    expect(retour.textContent).toContain('Retour à mes informations');
  });

  it('affiche l\'encart Ma cotisation avec Total TTC et franchise', () => {
    charger();
    const texte: string = fixture.nativeElement.textContent;
    expect(texte).toContain('Ma cotisation');
    expect(texte).toContain('Total TTC');
    expect(texte).toContain('Franchise');
    expect(texte).toContain('500');
  });

  it('affiche la phrase annuelle dans la cotisation', () => {
    charger();
    expect(fixture.nativeElement.textContent).toContain('sans frais de fractionnement');
  });

  it('affiche le récapitulatif avec 5 lignes et les liens Modifier', () => {
    charger();
    const libelles = fixture.nativeElement.querySelectorAll('.recap-liste__libelle');
    const textes = Array.from(libelles).map((el: any) => el.textContent.trim());
    expect(textes).toContain('Véhicule');
    expect(textes).toContain('Conducteur principal');
    expect(textes).toContain('Formule');
    expect(textes).toContain('Souscripteur');
    expect(textes).toContain('Paiement');
  });

  it('affiche l\'année de permis et le bonus-malus dans le récapitulatif', () => {
    charger();
    const texte: string = fixture.nativeElement.textContent;
    expect(texte).toContain('2010');
    expect(texte).toContain('0,68');
  });

  it('vérifie l\'IBAN au blur et affiche BIC et banque si valide', () => {
    charger();
    const ibanInput = fixture.nativeElement.querySelector('#iban') as HTMLInputElement;
    ibanInput.value = 'FR7630006000011234567890189';
    ibanInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    ibanInput.dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    const req = http.expectOne((r) => r.url.includes('/iban/verifier'));
    expect(req.request.body.iban).toBe('FR7630006000011234567890189');
    req.flush({ valide: true, ibanMasque: '•••• 0189', bic: 'BNPAFRPPXXX', banque: 'BNP Paribas', zoneSepa: true, messageErreur: null });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('BNPAFRPPXXX');
    expect(fixture.nativeElement.textContent).toContain('BNP Paribas');
  });

  it('affiche le lien Modifier après vérification IBAN réussie', () => {
    charger();
    const ibanInput = fixture.nativeElement.querySelector('#iban') as HTMLInputElement;
    ibanInput.value = 'FR7630006000011234567890189';
    ibanInput.dispatchEvent(new Event('input'));
    ibanInput.dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    const req = http.expectOne((r) => r.url.includes('/iban/verifier'));
    req.flush({ valide: true, ibanMasque: '•••• 0189', bic: 'BNPAFRPPXXX', banque: 'BNP Paribas', zoneSepa: true, messageErreur: null });
    fixture.detectChanges();

    const modifier = fixture.nativeElement.querySelector('.lien-modifier');
    expect(modifier).toBeTruthy();
    // L'IBAN complet ne doit pas apparaître
    expect(fixture.nativeElement.textContent).not.toContain('FR7630006000011234567890189');
  });

  it('affiche l\'erreur IBAN hors SEPA', () => {
    charger();
    const ibanInput = fixture.nativeElement.querySelector('#iban') as HTMLInputElement;
    ibanInput.value = 'BR1800360305000010009795493C1';
    ibanInput.dispatchEvent(new Event('input'));
    ibanInput.dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    const req = http.expectOne((r) => r.url.includes('/iban/verifier'));
    req.flush({ valide: false, ibanMasque: null, bic: null, banque: null, zoneSepa: false, messageErreur: "Cet IBAN n'est pas dans la zone SEPA : le prélèvement est impossible." });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('SEPA');
  });

  it('ligne Formule affiche le prix formule seule (hors options)', () => {
    charger(INIT_AVEC_OPTION);
    const ligneFormule = fixture.nativeElement.querySelector('.encart-devis__ligne--formule') as HTMLElement;
    expect(ligneFormule.textContent).toContain('18,99');
    expect(ligneFormule.textContent).not.toContain('22,49');
  });

  it('Total TTC affiche le total formule + options', () => {
    charger(INIT_AVEC_OPTION);
    const prix = fixture.nativeElement.querySelector('.encart-devis__prix') as HTMLElement;
    expect(prix.textContent).toContain('22,49');
  });

  it('phrase annuelle affiche 12 × le total mensuel', () => {
    charger(INIT_AVEC_OPTION);
    expect(fixture.nativeElement.textContent).toContain('269,88');
  });

  it('affiche "Ce champ est obligatoire." pour IBAN vide', () => {
    charger();
    const cas = fixture.nativeElement.querySelector('#autorisation') as HTMLInputElement;
    cas.click();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    const erreurs: string[] = Array.from(fixture.nativeElement.querySelectorAll('.erreur')).map((e: any) => e.textContent.trim());
    expect(erreurs).toContain('Ce champ est obligatoire.');
  });

  it('affiche "Vérifiez votre IBAN" quand IBAN saisi mais non vérifié', () => {
    charger();
    const ibanInput = fixture.nativeElement.querySelector('#iban') as HTMLInputElement;
    ibanInput.value = 'FR7630006000011234567890189';
    ibanInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const cas = fixture.nativeElement.querySelector('#autorisation') as HTMLInputElement;
    cas.click();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    const erreurs: string[] = Array.from(fixture.nativeElement.querySelectorAll('.erreur')).map((e: any) => e.textContent.trim());
    expect(erreurs).toContain('Vérifiez votre IBAN avant de continuer.');
  });

  it('n\'affiche pas le · si la plaque est absente', () => {
    charger({ ...INIT_REPONSE, vehiculePlaque: '' });
    const texte: string = fixture.nativeElement.textContent;
    expect(texte).not.toContain('Renault Clio ·');
    expect(texte).toContain('Renault Clio');
  });

  it('affiche la franchise sans décimales', () => {
    charger();
    const texte: string = fixture.nativeElement.textContent;
    expect(texte).toContain('500 €');
    expect(texte).not.toContain('500,00 €');
  });

  it('affiche l\'échéancier quand autorisation cochée et IBAN vérifié et jour choisi', () => {
    charger();

    // Vérifier IBAN
    const ibanInput = fixture.nativeElement.querySelector('#iban') as HTMLInputElement;
    ibanInput.value = 'FR7630006000011234567890189';
    ibanInput.dispatchEvent(new Event('input'));
    ibanInput.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
    const req = http.expectOne((r) => r.url.includes('/iban/verifier'));
    req.flush({ valide: true, ibanMasque: '•••• 0189', bic: 'BNPAFRPPXXX', banque: 'BNP Paribas', zoneSepa: true, messageErreur: null });
    fixture.detectChanges();

    // Date de début
    const dateInput = fixture.nativeElement.querySelector('#date-debut') as HTMLInputElement;
    const demain = new Date(); demain.setDate(demain.getDate() + 20);
    dateInput.value = demain.toISOString().split('T')[0];
    dateInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    // Jour de prélèvement
    const boutons = fixture.nativeElement.querySelectorAll('.choix__option') as NodeListOf<HTMLButtonElement>;
    boutons[1].click(); // Le 10
    fixture.detectChanges();

    // Cocher autorisation
    const cas = fixture.nativeElement.querySelector('#autorisation') as HTMLInputElement;
    cas.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.echeancier')).toBeTruthy();
  });

  it('affiche le bilan et le lien PDF après génération réussie', () => {
    charger();
    svc.enregistrerContrat({
      numero: 'AE-2026-000001',
      statut: 'EN_ATTENTE_DE_SIGNATURE',
      documentMandatSepaId: 7,
      premierPrelevement: '2026-12-15',
      montantMensuel: 55,
    });
    fixture.detectChanges();

    const texte: string = fixture.nativeElement.textContent;
    expect(texte).toContain('AE-2026-000001');
    expect(texte).toContain('15/12/2026');
  });

  it('n\'affiche pas le formulaire SEPA après génération du contrat', () => {
    charger();
    svc.enregistrerContrat({
      numero: 'AE-2026-000001', statut: 'EN_ATTENTE_DE_SIGNATURE',
      documentMandatSepaId: 7, premierPrelevement: '2026-12-15', montantMensuel: 55,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('affiche les erreurs après tentative de génération sans saisie', () => {
    charger();
    // Cocher l'autorisation pour pouvoir soumettre
    const cas = fixture.nativeElement.querySelector('#autorisation') as HTMLInputElement;
    cas.click();
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const erreurs = fixture.nativeElement.querySelectorAll('.erreur');
    expect(erreurs.length).toBeGreaterThan(0);
  });
});
