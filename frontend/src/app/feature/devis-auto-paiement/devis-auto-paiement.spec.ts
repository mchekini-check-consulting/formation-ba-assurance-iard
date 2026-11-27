import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { DevisAutoPaiement } from './devis-auto-paiement';
import { DevisAutoService } from '../../core/service/devis-auto.service';

const INIT_REPONSE = {
  souscripteurPrenom: 'Marie', souscripteurNom: 'Durand',
  souscripteurAdresse: '12 rue de la Paix', souscripteurCodePostal: '75001', souscripteurVille: 'Paris',
  souscripteurEmail: 'marie@test.fr',
  vehiculeMarque: 'Renault', vehiculeModele: 'Clio', vehiculePlaque: 'AA-123-BB',
  formule: 'TR', options: [], franchise: 500, prixMensuelTtc: 55, prixAnnuelTtc: 660,
  hamonApplicable: false, dateFinAncienContrat: null,
  ics: 'FR00ZZZ000000', creancier: "Assur'Easy",
  titulairePrerempli: 'Marie Durand', numeroContratExistant: null, documentMandatSepaIdExistant: null,
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

  it('affiche le récapitulatif souscripteur et véhicule', () => {
    charger();
    const texte: string = fixture.nativeElement.textContent;
    expect(texte).toContain('Marie Durand');
    expect(texte).toContain('Renault Clio');
    expect(texte).toContain('AA-123-BB');
  });

  it('pré-remplit le titulaire depuis la réponse', () => {
    charger();
    const titulaire = fixture.nativeElement.querySelector('#titulaire') as HTMLInputElement;
    expect(titulaire.value).toBe('Marie Durand');
  });

  it('affiche l\'encadré Loi Hamon si hamonApplicable', () => {
    charger({ ...INIT_REPONSE, hamonApplicable: true, dateFinAncienContrat: '2026-12-31' });
    expect(fixture.nativeElement.textContent).toContain('Hamon');
  });

  it('n\'affiche pas l\'encadré Hamon si non applicable', () => {
    charger();
    expect(fixture.nativeElement.textContent).not.toContain('Hamon');
  });

  it('affiche l\'encart Ma cotisation avec le prix mensuel', () => {
    charger();
    const texte: string = fixture.nativeElement.textContent;
    expect(texte).toContain('Ma cotisation');
    expect(texte).toContain('55');
  });

  it('affiche les erreurs après tentative de génération sans saisie', () => {
    charger();
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const erreurs = fixture.nativeElement.querySelectorAll('.erreur');
    expect(erreurs.length).toBeGreaterThan(0);
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

  it('envoie la vérification IBAN au clic sur Vérifier', () => {
    charger();
    const bouton = fixture.nativeElement.querySelector('.bouton-verif') as HTMLButtonElement;
    const ibanInput = fixture.nativeElement.querySelector('#iban') as HTMLInputElement;
    ibanInput.value = 'FR7630006000011234567890189';
    ibanInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    bouton.click();
    fixture.detectChanges();

    const req = http.expectOne((r) => r.url.includes('/iban/verifier'));
    expect(req.request.body.iban).toBe('FR7630006000011234567890189');
    req.flush({ valide: true, ibanMasque: '•••• 0189', bic: 'BNPAFRPPXXX', banque: 'BNP Paribas', messageErreur: null });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('IBAN accepté');
  });
});
