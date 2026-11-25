import { TestBed } from '@angular/core/testing';
import { AchatAuto, BesoinsAuto, ConducteurAuto, RecommandationAuto, UsageAutoSaisie, VehiculeAuto } from '../model/devis-auto';
import { DevisAutoService } from './devis-auto.service';

describe('DevisAutoService', () => {
  let service: DevisAutoService;

  const vehicule: VehiculeAuto = {
    refVehiculeId: 1,
    immatriculation: 'AB-123-CD',
    marque: 'Peugeot',
    modele: '308',
    energie: 'ESSENCE',
    version: '1.2',
    annee: 2021,
    valeurNeuve: 18500,
  };
  const achat: AchatAuto = { immatriculation: null, plaqueRenseignee: false, nonImmatricule: false, dateAcquisition: '15/06/2023', modeAchat: 'COMPTANT', titulaireCarteGrise: 'SOUSCRIPTEUR' };
  const usage: UsageAutoSaisie = { usage: 'PRIVE', stationnement: 'GARAGE_FERME', kmAnnuels: '8000_15000', codePostal: '75000' };
  const conducteur: ConducteurAuto = {
    conducteurPrincipal: 'MOI_MEME',
    dateNaissance: '15/06/1990',
    datePermis: '20/03/2010',
    bonusMalus: 0.9,
    anneesAssurance: 5,
    conduiteAccompagnee: false,
    nbSinistresResponsables: 0,
    nbSinistresNonResponsables: 0,
    resiliationAssureur: 'AUCUNE',
    suspensionAnnulation: false,
    condamnationAlcool: false,
    assureVehicule: 'NON',
    ancienAssureur: '',
    ancienNumeroContrat: '',
  };
  const besoins: BesoinsAuto = {
    valeurVehicule: 15000,
    vehiculeIndispensable: true,
    assistance0km: false,
    protectionConducteur: true,
    brisGlace: false,
    accompagnementLitige: false,
    priorite: 'PROTECTION_LARGE',
  };
  const recommandation: RecommandationAuto = {
    formuleConseillee: 'TOUS_RISQUES',
    raisons: ['Véhicule indispensable.'],
    prixMensuel: 45.00,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DevisAutoService);
  });

  it('enregistre le vehicule et reinitialise toutes les etapes suivantes', () => {
    service.achat.set(achat);
    service.usage.set(usage);
    service.devisAutoId.set(42);
    service.besoins.set(besoins);
    service.enregistrerVehicule(vehicule);
    expect(service.vehicule()).toEqual(vehicule);
    expect(service.achat()).toBeNull();
    expect(service.usage()).toBeNull();
    expect(service.devisAutoId()).toBeNull();
    expect(service.besoins()).toBeNull();
  });

  it("enregistre l'achat et reinitialise les etapes suivantes", () => {
    service.usage.set(usage);
    service.conducteur.set(conducteur);
    service.devisAutoId.set(42);
    service.enregistrerAchat(achat);
    expect(service.achat()).toEqual(achat);
    expect(service.usage()).toBeNull();
    expect(service.conducteur()).toBeNull();
    expect(service.devisAutoId()).toBeNull();
  });

  it("enregistre l'usage et reinitialise conducteur et devisAutoId", () => {
    service.conducteur.set(conducteur);
    service.devisAutoId.set(42);
    service.enregistrerUsage(usage);
    expect(service.usage()).toEqual(usage);
    expect(service.conducteur()).toBeNull();
    expect(service.devisAutoId()).toBeNull();
  });

  it('enregistre le conducteur sans toucher aux etapes precedentes', () => {
    service.vehicule.set(vehicule);
    service.achat.set(achat);
    service.enregistrerConducteur(conducteur);
    expect(service.conducteur()?.conducteurPrincipal).toBe('MOI_MEME');
    expect(service.vehicule()).toEqual(vehicule);
    expect(service.achat()).toEqual(achat);
  });

  it('enregistre le devisAutoId', () => {
    service.enregistrerDevisAutoId(123);
    expect(service.devisAutoId()).toBe(123);
  });

  it('enregistre les besoins et la recommandation', () => {
    service.enregistrerBesoinsEtRecommandation(besoins, recommandation);
    expect(service.besoins()).toEqual(besoins);
    expect(service.recommandation()?.formuleConseillee).toBe('TOUS_RISQUES');
  });

  it('enregistrerTarifEtape1 stocke et réinitialise avec le véhicule', () => {
    const service = TestBed.inject(DevisAutoService);
    const tarif = { tarifable: true, prix: [{ formule: 'TIERS', mensuel: 42 }] };
    service.enregistrerTarifEtape1(tarif as any);
    expect(service.tarifEtape1()?.prix[0].mensuel).toBe(42);
    service.enregistrerVehicule({ refVehiculeId: 1, immatriculation: null, marque: 'P', modele: '308', energie: 'ESSENCE', version: null, annee: 2021, valeurNeuve: 18500 });
    expect(service.tarifEtape1()).toBeNull();
  });
});
