import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { DevisAutoService } from '../service/devis-auto.service';
import { achatRenseigneGuard, besoinsRenseignesGuard, conducteurRenseigneGuard, usageRenseigneGuard, vehiculeRenseigneGuard } from './devis-auto.guard';
import { VehiculeAuto, AchatAuto, ConducteurAuto, UsageAutoSaisie } from '../model/devis-auto';

describe('gardes du devis auto', () => {
  let devisAuto: DevisAutoService;
  const route = {} as ActivatedRouteSnapshot;
  const etat = {} as RouterStateSnapshot;
  const adresse = (resultat: unknown): string => TestBed.inject(Router).serializeUrl(resultat as UrlTree);

  const vehicule: VehiculeAuto = { refVehiculeId: 1, immatriculation: null, marque: 'Peugeot', modele: '308', energie: 'ESSENCE', version: null, annee: 2021, valeurNeuve: 18500 };
  const achat: AchatAuto = { immatriculation: null, plaqueRenseignee: false, nonImmatricule: false, dateAcquisition: '15/06/2023', modeAchat: 'COMPTANT', titulaireCarteGrise: 'SOUSCRIPTEUR' };
  const usage: UsageAutoSaisie = { usage: 'PRIVE', stationnement: 'GARAGE_FERME', kmAnnuels: '8000_15000', codePostal: '75000' };
  const conducteur: ConducteurAuto = {
    conducteurPrincipal: 'MOI_MEME', dateNaissance: '15/06/1990', datePermis: '20/03/2010',
    bonusMalus: 0.9, anneesAssurance: 5, conduiteAccompagnee: false,
    nbSinistresResponsables: 0, nbSinistresNonResponsables: 0,
    resiliationAssureur: 'AUCUNE', suspensionAnnulation: false, condamnationAlcool: false,
    assureVehicule: 'NON', ancienAssureur: '', ancienNumeroContrat: '',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    devisAuto = TestBed.inject(DevisAutoService);
  });

  describe('vehiculeRenseigneGuard', () => {
    it("renvoie vers l'etape 1.1 si aucun vehicule", () => {
      const resultat = TestBed.runInInjectionContext(() => vehiculeRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/vehicule');
    });

    it('laisse passer une fois le vehicule renseigne', () => {
      devisAuto.vehicule.set(vehicule);
      expect(TestBed.runInInjectionContext(() => vehiculeRenseigneGuard(route, etat))).toBe(true);
    });
  });

  describe('achatRenseigneGuard', () => {
    it("renvoie vers l'etape 1.1 si aucun vehicule", () => {
      const resultat = TestBed.runInInjectionContext(() => achatRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/vehicule');
    });

    it("renvoie vers l'etape 1.2 si vehicule renseigne mais pas l'achat", () => {
      devisAuto.vehicule.set(vehicule);
      const resultat = TestBed.runInInjectionContext(() => achatRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/achat');
    });

    it("laisse passer une fois le vehicule et l'achat renseignes", () => {
      devisAuto.vehicule.set(vehicule);
      devisAuto.achat.set(achat);
      expect(TestBed.runInInjectionContext(() => achatRenseigneGuard(route, etat))).toBe(true);
    });
  });

  describe('usageRenseigneGuard', () => {
    it("renvoie vers l'etape 1.1 si aucun vehicule", () => {
      const resultat = TestBed.runInInjectionContext(() => usageRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/vehicule');
    });

    it("renvoie vers l'etape 1.2 si l'achat n'est pas renseigne", () => {
      devisAuto.vehicule.set(vehicule);
      const resultat = TestBed.runInInjectionContext(() => usageRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/achat');
    });

    it("renvoie vers l'etape 1.3 si l'usage n'est pas renseigne", () => {
      devisAuto.vehicule.set(vehicule);
      devisAuto.achat.set(achat);
      const resultat = TestBed.runInInjectionContext(() => usageRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/usage');
    });

    it('laisse passer une fois les trois etapes renseignees', () => {
      devisAuto.vehicule.set(vehicule);
      devisAuto.achat.set(achat);
      devisAuto.usage.set(usage);
      expect(TestBed.runInInjectionContext(() => usageRenseigneGuard(route, etat))).toBe(true);
    });
  });

  describe('conducteurRenseigneGuard', () => {
    it("renvoie vers l'etape 1.1 si aucun vehicule", () => {
      const resultat = TestBed.runInInjectionContext(() => conducteurRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/vehicule');
    });

    it("renvoie vers l'etape 1.2 si l'achat n'est pas renseigne", () => {
      devisAuto.vehicule.set(vehicule);
      const resultat = TestBed.runInInjectionContext(() => conducteurRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/achat');
    });

    it("renvoie vers l'etape 1.3 si l'usage n'est pas renseigne", () => {
      devisAuto.vehicule.set(vehicule);
      devisAuto.achat.set(achat);
      const resultat = TestBed.runInInjectionContext(() => conducteurRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/usage');
    });

    it("renvoie vers l'etape 1.4 si le conducteur n'est pas renseigne", () => {
      devisAuto.vehicule.set(vehicule);
      devisAuto.achat.set(achat);
      devisAuto.usage.set(usage);
      const resultat = TestBed.runInInjectionContext(() => conducteurRenseigneGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/conducteur');
    });

    it('laisse passer une fois les quatre etapes renseignees', () => {
      devisAuto.vehicule.set(vehicule);
      devisAuto.achat.set(achat);
      devisAuto.usage.set(usage);
      devisAuto.enregistrerConducteur(conducteur);
      expect(TestBed.runInInjectionContext(() => conducteurRenseigneGuard(route, etat))).toBe(true);
    });
  });

  describe('besoinsRenseignesGuard', () => {
    it("renvoie vers l'etape 1.1 si aucun vehicule", () => {
      const resultat = TestBed.runInInjectionContext(() => besoinsRenseignesGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/vehicule');
    });

    it("renvoie vers l'etape 1.4 si le conducteur n'est pas renseigne", () => {
      devisAuto.vehicule.set(vehicule);
      devisAuto.achat.set(achat);
      devisAuto.usage.set(usage);
      const resultat = TestBed.runInInjectionContext(() => besoinsRenseignesGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/conducteur');
    });

    it("renvoie vers les besoins si la recommandation n'est pas disponible", () => {
      devisAuto.vehicule.set(vehicule);
      devisAuto.achat.set(achat);
      devisAuto.usage.set(usage);
      devisAuto.enregistrerConducteur(conducteur);
      const resultat = TestBed.runInInjectionContext(() => besoinsRenseignesGuard(route, etat));
      expect(adresse(resultat)).toBe('/devis-auto/besoins');
    });

    it('laisse passer une fois les besoins renseignes', () => {
      devisAuto.vehicule.set(vehicule);
      devisAuto.achat.set(achat);
      devisAuto.usage.set(usage);
      devisAuto.enregistrerConducteur(conducteur);
      devisAuto.enregistrerBesoinsEtRecommandation(
        { valeurVehicule: 15000, vehiculeIndispensable: true, assistance0km: false, protectionConducteur: true, brisGlace: false, accompagnementLitige: false, priorite: 'PROTECTION_LARGE' },
        { formuleConseillee: 'TOUS_RISQUES', raisons: ['Vehicule indispensable.'], prixMensuel: 45.00 },
      );
      expect(TestBed.runInInjectionContext(() => besoinsRenseignesGuard(route, etat))).toBe(true);
    });
  });
});
