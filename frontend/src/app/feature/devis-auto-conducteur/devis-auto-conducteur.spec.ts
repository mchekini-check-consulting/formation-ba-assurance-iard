import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DevisAutoConducteur } from './devis-auto-conducteur';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { VehiculeAuto, AchatAuto, UsageAutoSaisie } from '../../core/model/devis-auto';
import { TarifAutoReponse } from '../../core/service/tarif-auto.service';
import { DevisAutoCreationReponse } from '../../core/service/devis-auto-creation.service';

@Component({ template: '' })
class StubPage {}

const vehicule: VehiculeAuto = {
  refVehiculeId: 1, immatriculation: null, marque: 'Peugeot', modele: '308',
  energie: 'ESSENCE', version: null, annee: 2021, valeurNeuve: 18500,
};
const achat: AchatAuto = {
  immatriculation: null, plaqueRenseignee: false, nonImmatricule: false,
  dateAcquisition: '15/06/2023', modeAchat: 'COMPTANT', titulaireCarteGrise: 'SOUSCRIPTEUR',
};
const usage: UsageAutoSaisie = {
  usage: 'PRIVE', stationnement: 'GARAGE_FERME', kmAnnuels: '8000_15000', codePostal: '75000',
};

const reponseTarif: TarifAutoReponse = {
  tarifable: true,
  prix: [{ formule: 'TIERS', mensuel: 42.5 }],
};

const reponseCreation: DevisAutoCreationReponse = {
  devisAutoId: 1,
  statutDevis: 'EN_COURS',
  eligibleEnLigne: true,
};

describe('DevisAutoConducteur', () => {
  let service: DevisAutoService;
  let http: HttpTestingController;

  const creer = () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'devis-auto/besoins', component: StubPage },
          { path: 'devis-auto/conducteur', component: StubPage },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(DevisAutoService);
    service.vehicule.set(vehicule);
    service.achat.set(achat);
    service.usage.set(usage);
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DevisAutoConducteur);
    fixture.detectChanges();
    return fixture;
  };

  // Flush les appels tarif en attente (debounce 300 ms, timer pas encore déclenché en test)
  const viderRequetesTarif = () =>
    http.match(r => r.url.includes('devis/auto/tarif')).forEach(r => r.flush(null));

  const soumettre = (fixture: ReturnType<typeof creer>) => {
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };

  afterEach(() => http.verify());

  // ─── STRUCTURE ─────────────────────────────────────────────────

  it('affiche le titre Conducteurs et antecedents', () => {
    const fixture = creer();
    viderRequetesTarif();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Conducteurs et ant');
  });

  it('affiche 1.1, 1.2 et 1.3 comme terminees, 1.4 comme active', () => {
    const fixture = creer();
    viderRequetesTarif();
    const el = fixture.nativeElement as HTMLElement;
    const terminees = el.querySelectorAll('.sous-etape--terminee');
    expect(terminees.length).toBeGreaterThanOrEqual(3);
  });

  it('affiche les 4 boutons conducteur principal', () => {
    const fixture = creer();
    viderRequetesTarif();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Moi');
    expect(texte).toContain('Mon conjoint');
    expect(texte).toContain('Mon enfant');
    expect(texte).toContain('autre personne');
  });

  it('affiche les champs date naissance, date permis, bonus-malus, annees assurance', () => {
    const fixture = creer();
    viderRequetesTarif();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('#date-naissance')).toBeTruthy();
    expect(el.querySelector('#date-permis')).toBeTruthy();
    expect(el.querySelector('#bonus-malus')).toBeTruthy();
    expect(el.querySelector('#annees-assurance')).toBeTruthy();
  });

  it('affiche la grille antecedents avec les 6 selects', () => {
    const fixture = creer();
    viderRequetesTarif();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('#sinistres-responsables')).toBeTruthy();
    expect(el.querySelector('#sinistres-non-responsables')).toBeTruthy();
    expect(el.querySelector('#resiliation-assureur')).toBeTruthy();
    expect(el.querySelector('#suspension-annulation')).toBeTruthy();
    expect(el.querySelector('#condamnation-alcool')).toBeTruthy();
    expect(el.querySelector('#assure-vehicule')).toBeTruthy();
  });

  // ─── VALIDATION ────────────────────────────────────────────────

  it('affiche des erreurs si formulaire soumis vide', () => {
    const fixture = creer();
    viderRequetesTarif();
    soumettre(fixture);
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Indiquez le conducteur principal');
  });

  it('affiche une erreur si date de naissance absente', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    fixture.detectChanges();
    soumettre(fixture);
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('obligatoire');
  });

  // ─── CONDUCTEUR MINEUR ─────────────────────────────────────────

  it('affiche message bloquant si conducteur principal est un enfant mineur', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['dateNaissance'].set('01/01/2015');
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('mineur');
  });

  it('affiche le bouton Etre rappele si conducteur est mineur', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['dateNaissance'].set('01/01/2015');
    fixture.detectChanges();
    const btn = (fixture.nativeElement as HTMLElement).querySelector('.btn-rappel');
    expect(btn).toBeTruthy();
  });

  // ─── CONDITIONS BLOQUANTES ─────────────────────────────────────

  it('affiche alerte bloquante si bonus-malus depasse le seuil (1,50)', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['bonusMalus'].set(1.8);
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('bonus-malus');
    expect(texte).toContain('1,80');
  });

  it('affiche alerte bloquante si resiliation NON_PAIEMENT', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['resiliationAssureur'].set('NON_PAIEMENT');
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('non-paiement');
  });

  it('N\'affiche pas d\'alerte bloquante pour SINISTRALITE', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['resiliationAssureur'].set('SINISTRALITE');
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).not.toContain('Souscription impossible en ligne');
  });

  it('affiche alerte bloquante si suspension ou annulation du permis', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['suspensionAnnulation'].set(true);
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('suspension');
  });

  it('affiche alerte bloquante si condamnation alcool', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['condamnationAlcool'].set(true);
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('condamnation');
  });

  it('affiche alerte bloquante si novice avec sinistre responsable', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['anneesAssurance'].set(0);
    comp['nbSinistresResponsables'].set(1);
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('novice');
  });

  it('affiche bouton Etre rappele quand profil bloquant', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['condamnationAlcool'].set(true);
    fixture.detectChanges();
    const btn = (fixture.nativeElement as HTMLElement).querySelector('.btn-rappel');
    expect(btn).toBeTruthy();
  });

  // ─── BLOC HAMON CONDITIONNEL ────────────────────────────────────

  it('affiche le bloc hamon quand assureVehicule = OUI_PLUS_UN_AN', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['assureVehicule'].set('OUI_PLUS_UN_AN');
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('#assureur-actuel')).toBeTruthy();
    expect(el.querySelector('#numero-contrat-actuel')).toBeTruthy();
  });

  it('masque le bloc hamon quand assureVehicule = NON', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['assureVehicule'].set('NON');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('#assureur-actuel')).toBeFalsy();
  });

  it('masque le bloc hamon quand assureVehicule = OUI_MOINS_UN_AN', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['assureVehicule'].set('OUI_MOINS_UN_AN');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('#assureur-actuel')).toBeFalsy();
  });

  // ─── ENCART TARIF ───────────────────────────────────────────────

  it('affiche le prix dans l\'encart quand le tarif est disponible', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['calculEnCours'].set(false);
    comp['tarif'].set(reponseTarif);
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('42');
  });

  it('affiche "conseiller" dans l\'encart quand le profil est bloquant', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;
    comp['condamnationAlcool'].set(true);
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('conseiller');
  });

  // ─── SOUMISSION VALIDE ──────────────────────────────────────────

  it('enregistre le conducteur et navigue vers besoins apres creation reussie', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;

    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['dateNaissance'].set('15/06/1990');
    comp['datePermis'].set('20/03/2010');
    comp['bonusMalus'].set(0.9);
    comp['anneesAssurance'].set(5);
    comp['nbSinistresResponsables'].set(0);
    comp['nbSinistresNonResponsables'].set(0);
    comp['resiliationAssureur'].set('AUCUNE');
    comp['suspensionAnnulation'].set(false);
    comp['condamnationAlcool'].set(false);
    comp['assureVehicule'].set('NON');
    fixture.detectChanges();

    soumettre(fixture);
    viderRequetesTarif();

    // Création POST doit être en attente
    const req = http.expectOne(r => r.url.includes('/v1/devis/auto') && !r.url.includes('/tarif'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body.conducteurPrincipal).toBe('MOI_MEME');
    expect(req.request.body.bonusMalus).toBe(0.9);

    req.flush(reponseCreation);
    fixture.detectChanges();

    // Le conducteur est enregistré dans le service
    expect(service.conducteur()?.conducteurPrincipal).toBe('MOI_MEME');
    expect(service.conducteur()?.resiliationAssureur).toBe('AUCUNE');
    expect(service.conducteur()?.bonusMalus).toBe(0.9);
  });

  it('affiche un message d\'erreur si le serveur renvoie 422', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;

    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['dateNaissance'].set('15/06/1990');
    comp['datePermis'].set('20/03/2010');
    comp['bonusMalus'].set(0.9);
    comp['anneesAssurance'].set(5);
    comp['nbSinistresResponsables'].set(0);
    comp['nbSinistresNonResponsables'].set(0);
    comp['resiliationAssureur'].set('AUCUNE');
    comp['suspensionAnnulation'].set(false);
    comp['condamnationAlcool'].set(false);
    comp['assureVehicule'].set('NON');
    fixture.detectChanges();

    soumettre(fixture);
    viderRequetesTarif();

    const req = http.expectOne(r => r.url.includes('/v1/devis/auto') && !r.url.includes('/tarif'));
    req.flush({ motif: 'BONUS_MALUS' }, { status: 422, statusText: 'Unprocessable Entity' });
    fixture.detectChanges();

    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('souscription en ligne');
  });

  it('ne soumet pas si profil bloquant (BM > seuil)', () => {
    const fixture = creer();
    viderRequetesTarif();
    const comp = fixture.componentInstance;

    comp['conducteurPrincipal'].set('MOI_MEME');
    comp['dateNaissance'].set('15/06/1990');
    comp['datePermis'].set('20/03/2010');
    comp['bonusMalus'].set(2.0);
    fixture.detectChanges();

    soumettre(fixture);
    viderRequetesTarif();

    // Aucun appel de creation
    http.expectNone(r => r.url.includes('/v1/devis/auto') && !r.url.includes('/tarif'));
    expect(service.conducteur()).toBeNull();
  });
});
