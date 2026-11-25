import { TestBed } from '@angular/core/testing';
import { AchatAuto, ConducteurAuto, UsageAutoSaisie, VehiculeAuto } from '../model/devis-auto';
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

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DevisAutoService);
  });

  it('enregistre le vehicule et reinitialise les etapes suivantes', () => {
    service.achat.set(achat);
    service.usage.set(usage);
    service.enregistrerVehicule(vehicule);
    expect(service.vehicule()).toEqual(vehicule);
    expect(service.achat()).toBeNull();
    expect(service.usage()).toBeNull();
  });

  it("enregistre l'achat et reinitialise usage et conducteur", () => {
    service.usage.set(usage);
    service.conducteur.set(conducteur);
    service.enregistrerAchat(achat);
    expect(service.achat()).toEqual(achat);
    expect(service.usage()).toBeNull();
    expect(service.conducteur()).toBeNull();
  });

  it("enregistre l'usage et reinitialise le conducteur", () => {
    service.conducteur.set(conducteur);
    service.enregistrerUsage(usage);
    expect(service.usage()).toEqual(usage);
    expect(service.conducteur()).toBeNull();
  });

  it('enregistre le conducteur sans toucher aux etapes precedentes', () => {
    service.vehicule.set(vehicule);
    service.achat.set(achat);
    service.enregistrerConducteur(conducteur);
    expect(service.conducteur()?.conducteurPrincipal).toBe('MOI_MEME');
    expect(service.conducteur()?.bonusMalus).toBe(0.9);
    expect(service.vehicule()).toEqual(vehicule);
    expect(service.achat()).toEqual(achat);
  });
});
