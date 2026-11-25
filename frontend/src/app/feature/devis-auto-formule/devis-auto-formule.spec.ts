import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { DevisAutoFormule } from './devis-auto-formule';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { RecommandationAuto } from '../../core/model/devis-auto';

@Component({ template: '' })
class StubInformations {}

const RECOMMANDATION_INTER: RecommandationAuto = {
  formuleConseillee: 'INTERMEDIAIRE',
  raisons: ['Véhicule récent', 'Usage professionnel'],
  prixMensuel: 52,
};

const RECOMMANDATION_TR: RecommandationAuto = {
  formuleConseillee: 'TOUS_RISQUES',
  raisons: ['Véhicule de grande valeur'],
  prixMensuel: 85,
};

const PRIX_REPONSE = {
  idTarif: 'abc',
  versionGrille: 'v1',
  dateValidite: '2027-01-01',
  prix: [
    { formule: 'TIERS', mensuel: 20, annuel: 220, franchise: null },
    { formule: 'INTER', mensuel: 50, annuel: 550, franchise: 400 },
    { formule: 'TR', mensuel: 80, annuel: 880, franchise: 500 },
  ],
  prixOptions: [
    { option: 'ASSISTANCE_0KM', mensuel: 3 },
    { option: 'VEHICULE_REMPLACEMENT', mensuel: 8 },
    { option: 'PROTECTION_CONDUCTEUR', mensuel: 5 },
    { option: 'PROTECTION_JURIDIQUE', mensuel: 4 },
  ],
};

describe('DevisAutoFormule', () => {
  let fixture: ComponentFixture<DevisAutoFormule>;
  let component: DevisAutoFormule;
  let svc: DevisAutoService;
  let http: HttpTestingController;

  function init(recommandation: RecommandationAuto = RECOMMANDATION_INTER): void {
    svc.devisAutoId.set(42);
    svc.recommandation.set(recommandation);
    fixture.detectChanges();
    const req = http.expectOne(r => r.url.includes('/prix-formules'));
    req.flush(PRIX_REPONSE);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DevisAutoFormule],
      providers: [
        provideRouter([{ path: 'devis-auto/informations', component: StubInformations }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DevisAutoFormule);
    component = fixture.componentInstance;
    svc = TestBed.inject(DevisAutoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('affiche les 3 cartes de formule après chargement', () => {
    init();
    const cartes = fixture.nativeElement.querySelectorAll('.carte-formule');
    expect(cartes.length).toBe(3);
  });

  it('affiche le badge "Conseillée" sur la carte INTER quand recommandation = INTERMEDIAIRE', () => {
    init(RECOMMANDATION_INTER);
    const badges = fixture.nativeElement.querySelectorAll('.badge-conseillee');
    expect(badges.length).toBe(1);
    const texte = fixture.nativeElement.textContent as string;
    expect(texte).toContain('Conseillée');
    // La carte active par défaut est INTER
    expect(component['formuleSelectionnee']()).toBe('INTER');
  });

  it('pré-sélectionne TR quand recommandation = TOUS_RISQUES', () => {
    init(RECOMMANDATION_TR);
    expect(component['formuleSelectionnee']()).toBe('TR');
  });

  it('toggle mensuel/annuel change l\'unité de prix affichée', () => {
    init();
    expect(component['periodicite']()).toBe('mensuel');
    component['choisirPeriodicite']('annuel');
    fixture.detectChanges();
    expect(component['periodicite']()).toBe('annuel');
    expect(component['unitePrix']()).toBe('/an');
  });

  it('déclenche l\'alerte B3 si on choisit une formule moins couvrante que la recommandation', () => {
    init(RECOMMANDATION_TR);
    // TR recommandé, on choisit INTER → moins couvrant
    component['choisir']('INTER');
    fixture.detectChanges();
    expect(component['alerteChoixMoinsCourant']()).toBe(true);
    const alerte = fixture.nativeElement.querySelector('.alerte-choix');
    expect(alerte).toBeTruthy();
  });

  it('pas d\'alerte B3 si on choisit la formule conseillée', () => {
    init(RECOMMANDATION_TR);
    // TR recommandé, TR sélectionné → pas d\'alerte
    expect(component['alerteChoixMoinsCourant']()).toBe(false);
  });

  it('retire VEHICULE_REMPLACEMENT quand on choisit TIERS', () => {
    init(RECOMMANDATION_INTER);
    component['toggleOption']('VEHICULE_REMPLACEMENT');
    expect(component['optionsChoisies']()).toContain('VEHICULE_REMPLACEMENT');
    component['choisir']('TIERS');
    expect(component['optionsChoisies']()).not.toContain('VEHICULE_REMPLACEMENT');
    expect(component['messageVehiculeRemplacement']()).toBeTruthy();
  });

  it('VEHICULE_REMPLACEMENT est incompatible avec la formule TIERS', () => {
    init(RECOMMANDATION_INTER);
    component['choisir']('TIERS');
    expect(component['optionCompatible']('VEHICULE_REMPLACEMENT')).toBe(false);
  });

  it('calcule correctement le total mensuel avec options', () => {
    init(RECOMMANDATION_INTER);
    // INTER = 50/mois, ASSISTANCE_0KM = 3/mois
    component['toggleOption']('ASSISTANCE_0KM');
    const total = component['calculerTotal']();
    expect(total.mensuel).toBe(53);
  });

  it('calcule correctement le total annuel', () => {
    init(RECOMMANDATION_INTER);
    component['choisirPeriodicite']('annuel');
    // INTER annuel = 550, pas d\'option
    const total = component['calculerTotal']();
    expect(total.annuel).toBe(550);
  });

  it('peutSouscrire est faux si alerte non confirmée', () => {
    init(RECOMMANDATION_TR);
    component['choisir']('TIERS');
    expect(component['peutSouscrire']()).toBe(false);
  });

  it('peutSouscrire est vrai après confirmation de l\'alerte', () => {
    init(RECOMMANDATION_TR);
    component['choisir']('TIERS');
    component['confirmerAlerte']();
    expect(component['peutSouscrire']()).toBe(true);
  });

  it('affiche le nom de la formule conseillée dans le sous-titre conseil', () => {
    init(RECOMMANDATION_INTER);
    const conseil = fixture.nativeElement.querySelector('.sous-titre-conseil');
    expect(conseil).toBeTruthy();
    expect((conseil.textContent as string)).toContain('Intermédiaire');
  });

  it('affiche le total dans le sidebar "Mon devis"', () => {
    init(RECOMMANDATION_INTER);
    const encart = fixture.nativeElement.querySelector('.encart-devis');
    expect(encart).toBeTruthy();
    // INTER = 50/mois → "50,00"
    expect(encart.textContent).toContain('50,00');
  });

  it('ouvre et ferme les exclusions d\'une formule', () => {
    init();
    expect(fixture.nativeElement.querySelector('.exclusions')).toBeNull();
    component['toggleExclusions']('TIERS');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.exclusions')).toBeTruthy();
    component['toggleExclusions']('TIERS');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.exclusions')).toBeNull();
  });

  it('formaterPrix formate avec 2 décimales et virgule française', () => {
    expect(component['formaterPrix'](45)).toBe('45,00');
    expect(component['formaterPrix'](null)).toBe('–');
  });

  it('total INTER + vehiculeRemplacement + protectionConducteur + protectionJuridique = 40,42', () => {
    // Vérifie les vrais prix de la DB (bug 2) : 28,92 + 4,00 + 5,00 + 2,50
    svc.devisAutoId.set(42);
    svc.recommandation.set(RECOMMANDATION_INTER);
    fixture.detectChanges();
    const req = http.expectOne(r => r.url.includes('/prix-formules'));
    req.flush({
      idTarif: 'x', versionGrille: 'AUTO-2026.1', dateValidite: '2027-01-01',
      prix: [
        { formule: 'TIERS',  mensuel: 15.00, annuel: 180.00, franchise: null },
        { formule: 'INTER',  mensuel: 28.92, annuel: 347.04, franchise: 400 },
        { formule: 'TR',     mensuel: 55.00, annuel: 660.00, franchise: 500 },
      ],
      prixOptions: [
        { option: 'ASSISTANCE_0KM',       mensuel: 3.50 },
        { option: 'VEHICULE_REMPLACEMENT', mensuel: 4.00 },
        { option: 'PROTECTION_CONDUCTEUR', mensuel: 5.00 },
        { option: 'PROTECTION_JURIDIQUE',  mensuel: 2.50 },
      ],
    });
    fixture.detectChanges();

    component['toggleOption']('VEHICULE_REMPLACEMENT');
    component['toggleOption']('PROTECTION_CONDUCTEUR');
    component['toggleOption']('PROTECTION_JURIDIQUE');

    const total = component['calculerTotal']();
    expect(total.mensuel).toBeCloseTo(40.42, 2);
  });

  it('affiche un message d\'erreur si le chargement échoue', () => {
    svc.devisAutoId.set(42);
    svc.recommandation.set(RECOMMANDATION_INTER);
    fixture.detectChanges();
    const req = http.expectOne(r => r.url.includes('/prix-formules'));
    req.error(new ProgressEvent('error'));
    fixture.detectChanges();
    const erreur = fixture.nativeElement.querySelector('.alerte-bloquante');
    expect(erreur).toBeTruthy();
  });

  it('envoie la bonne requête PUT au moment de la soumission', () => {
    init(RECOMMANDATION_INTER);
    component['confirmerAlerte']();
    component['soumettre']();
    const req = http.expectOne(r => r.method === 'PUT' && r.url.includes('/formule'));
    expect(req.request.body.formuleChoisie).toBe('INTER');
    req.flush({
      formuleChoisie: 'INTER',
      optionsChoisies: [],
      prixMensuelTtc: 50,
      prixAnnuelTtc: 550,
      dateFinValidite: '2027-01-01',
    });
    fixture.detectChanges();
  });
});
