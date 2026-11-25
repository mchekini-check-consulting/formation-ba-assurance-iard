import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DevisAutoAchat } from './devis-auto-achat';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { VehiculeAuto, AchatAuto } from '../../core/model/devis-auto';

@Component({ template: '' })
class StubPage {}

const vehiculePlaque: VehiculeAuto = {
  refVehiculeId: 1,
  immatriculation: 'GH-789-JK',
  marque: 'Citroën',
  modele: 'C3',
  energie: 'ESSENCE',
  version: '1.2 PureTech 83 Shine',
  annee: 2024,
  valeurNeuve: 15000,
};

const vehiculeMarque: VehiculeAuto = {
  refVehiculeId: 2,
  immatriculation: null,
  marque: 'Peugeot',
  modele: '308',
  energie: 'ESSENCE',
  version: null,
  annee: 2021,
  valeurNeuve: 18500,
};

describe('DevisAutoAchat', () => {
  let service: DevisAutoService;

  const creer = (vehicule: VehiculeAuto, achat: AchatAuto | null = null) => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'devis-auto/usage', component: StubPage }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(DevisAutoService);
    service.vehicule.set(vehicule);
    if (achat) service.achat.set(achat);
    const fixture = TestBed.createComponent(DevisAutoAchat);
    fixture.detectChanges();
    return fixture;
  };

  const soumettre = (fixture: ReturnType<typeof creer>) => {
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };

  // ─── CAS 1 : PLAQUE GH-789-JK ──────────────────────────────────

  it('plaque : affiche le stepper 6 etapes, Etape 1 active', () => {
    const fixture = creer(vehiculePlaque);
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Mon véhicule');
    expect(texte).toContain('Mes besoins');
    expect(texte).toContain('Ma formule');
  });

  it('plaque : affiche le badge avec la plaque GH-789-JK', () => {
    const fixture = creer(vehiculePlaque);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('GH-789-JK');
  });

  it('plaque : affiche les donnees du vehicule (marque, modele, energie)', () => {
    const fixture = creer(vehiculePlaque);
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Citroën');
    expect(texte).toContain('C3');
    expect(texte).toContain('Essence');
  });

  it('plaque : ne montre pas le champ immatriculation', () => {
    const fixture = creer(vehiculePlaque);
    expect((fixture.nativeElement as HTMLElement).querySelector('#immatriculation')).toBeNull();
  });

  it('plaque : affiche la question "Est-ce bien votre vehicule ?"', () => {
    const fixture = creer(vehiculePlaque);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Est-ce bien votre véhicule');
  });

  // ─── CAS 2 : CHOIX PAR MARQUE ──────────────────────────────────

  it('marque : affiche "Vehicule selectionne" sans question de confirmation', () => {
    const fixture = creer(vehiculeMarque);
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Véhicule sélectionné');
    expect(texte).not.toContain('Est-ce bien votre véhicule');
  });

  it('marque : affiche le champ immatriculation', () => {
    const fixture = creer(vehiculeMarque);
    expect((fixture.nativeElement as HTMLElement).querySelector('#immatriculation')).toBeTruthy();
  });

  it('marque : affiche le lien Modifier', () => {
    const fixture = creer(vehiculeMarque);
    const liens = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a'));
    expect(liens.some(l => l.textContent?.includes('Modifier'))).toBe(true);
  });

  // ─── CAS 3 : SUIVANT SANS RIEN REMPLIR ─────────────────────────

  it('plaque : affiche toutes les erreurs si formulaire vide', () => {
    const fixture = creer(vehiculePlaque);
    soumettre(fixture);
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Confirmez si ce véhicule est le vôtre');
    expect(texte).toContain("Indiquez la date d'acquisition");
    expect(texte).toContain("Indiquez votre mode d'achat");
    expect(texte).toContain('Indiquez le titulaire de la carte grise');
  });

  it('marque : affiche les erreurs obligatoires si formulaire vide (immatriculation facultative)', () => {
    const fixture = creer(vehiculeMarque);
    soumettre(fixture);
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).not.toContain("Renseignez l'immatriculation");
    expect(texte).toContain("Indiquez la date d'acquisition");
    expect(texte).toContain("Indiquez votre mode d'achat");
    expect(texte).toContain('Indiquez le titulaire de la carte grise');
  });

  // ─── CAS 4 : CE N'EST PAS MON VÉHICULE ─────────────────────────

  it('plaque : "Ce n\'est pas mon vehicule" affiche message et lien marque', () => {
    const fixture = creer(vehiculePlaque);
    const boutons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));
    const refusBouton = boutons.find(b => b.textContent?.includes("Ce n'est pas mon véhicule"))!;
    refusBouton.click();
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Rechercher par marque');
  });

  it('plaque : blocage si vehicule non confirme et tentative de continuer', () => {
    const fixture = creer(vehiculePlaque);
    const boutons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));
    boutons.find(b => b.textContent?.includes("Ce n'est pas mon véhicule"))!.click();
    fixture.detectChanges();
    soumettre(fixture);
    expect(service.achat()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Retrouvez votre véhicule par sa marque');
  });

  // ─── CAS 5 : FORMULAIRE COMPLET → ÉTAPE 1.3 ───────────────────

  it('plaque : soumission valide enregistre les donnees et navigue', () => {
    const fixture = creer(vehiculePlaque);
    const comp = fixture.componentInstance;

    comp['vehiculeConfirme'].set(true);
    comp['dateAcquisition'].set('15/06/2024');
    comp['modeAchat'].set('COMPTANT');
    comp['titulaireCarteGrise'].set('SOUSCRIPTEUR');
    fixture.detectChanges();

    soumettre(fixture);

    expect(service.achat()?.modeAchat).toBe('COMPTANT');
    expect(service.achat()?.titulaireCarteGrise).toBe('SOUSCRIPTEUR');
    expect(service.achat()?.dateAcquisition).toBe('15/06/2024');
    expect(service.achat()?.immatriculation).toBeNull();
  });

  it('marque : soumission valide avec immatriculation', () => {
    const fixture = creer(vehiculeMarque);
    const comp = fixture.componentInstance;

    comp['immatriculationSaisie'].set('AB-123-CD');
    comp['dateAcquisition'].set('10/03/2021');
    comp['modeAchat'].set('LOA');
    comp['titulaireCarteGrise'].set('CONJOINT');
    fixture.detectChanges();

    soumettre(fixture);

    expect(service.achat()?.immatriculation).toBe('AB-123-CD');
    expect(service.achat()?.modeAchat).toBe('LOA');
  });

  it('date invalide affiche erreur', () => {
    const fixture = creer(vehiculePlaque);
    const comp = fixture.componentInstance;
    comp['dateAcquisition'].set('99/99/9999');
    fixture.detectChanges();
    soumettre(fixture);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain("n'est pas valide");
  });

  it('immatriculation mauvais format affiche erreur', () => {
    const fixture = creer(vehiculeMarque);
    const comp = fixture.componentInstance;
    comp['immatriculationSaisie'].set('ABC123');
    fixture.detectChanges();
    soumettre(fixture);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Format attendu : AB-123-CD');
  });

  it('marque : soumission valide sans immatriculation (facultatif)', () => {
    const fixture = creer(vehiculeMarque);
    const comp = fixture.componentInstance;

    comp['dateAcquisition'].set('10/03/2021');
    comp['modeAchat'].set('COMPTANT');
    comp['titulaireCarteGrise'].set('SOUSCRIPTEUR');
    fixture.detectChanges();

    soumettre(fixture);

    expect(service.achat()?.immatriculation).toBeNull();
    expect(service.achat()?.plaqueRenseignee).toBe(false);
    expect(service.achat()?.nonImmatricule).toBe(false);
  });

  it('marque : case "non immatricule" coche masque le champ immatriculation', () => {
    const fixture = creer(vehiculeMarque);
    const comp = fixture.componentInstance;

    expect((fixture.nativeElement as HTMLElement).querySelector('#immatriculation')).toBeTruthy();

    comp['nonImmatricule'].set(true);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('#immatriculation')).toBeNull();
  });

  it('marque : case "non immatricule" cochee enregistre nonImmatricule=true', () => {
    const fixture = creer(vehiculeMarque);
    const comp = fixture.componentInstance;

    comp['nonImmatricule'].set(true);
    comp['dateAcquisition'].set('10/03/2021');
    comp['modeAchat'].set('COMPTANT');
    comp['titulaireCarteGrise'].set('SOUSCRIPTEUR');
    fixture.detectChanges();

    soumettre(fixture);

    expect(service.achat()?.nonImmatricule).toBe(true);
    expect(service.achat()?.immatriculation).toBeNull();
  });

  // ─── CAS 6 : RETOUR → DONNÉES CONSERVÉES ──────────────────────

  it('plaque : pre-remplit depuis les donnees deja enregistrees', () => {
    const achat: AchatAuto = {
      immatriculation: null,
      plaqueRenseignee: true,
      nonImmatricule: false,
      dateAcquisition: '20/07/2024',
      modeAchat: 'CREDIT',
      titulaireCarteGrise: 'CONJOINT',
    };
    const fixture = creer(vehiculePlaque, achat);
    const comp = fixture.componentInstance;
    expect(comp['modeAchat']()).toBe('CREDIT');
    expect(comp['titulaireCarteGrise']()).toBe('CONJOINT');
    expect(comp['dateAcquisition']()).toBe('20/07/2024');
    expect(comp['vehiculeConfirme']()).toBe(true);
  });

  it('marque : pre-remplit depuis les donnees deja enregistrees', () => {
    const achat: AchatAuto = {
      immatriculation: 'AB-123-CD',
      plaqueRenseignee: true,
      nonImmatricule: false,
      dateAcquisition: '10/03/2021',
      modeAchat: 'LOA',
      titulaireCarteGrise: 'SOUSCRIPTEUR',
    };
    const fixture = creer(vehiculeMarque, achat);
    const comp = fixture.componentInstance;
    expect(comp['immatriculationSaisie']()).toBe('AB-123-CD');
    expect(comp['modeAchat']()).toBe('LOA');
    expect(comp['dateAcquisition']()).toBe('10/03/2021');
  });
});
