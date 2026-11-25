import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { Component } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { DevisAutoBesoins } from './devis-auto-besoins';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { BesoinsAutoService } from '../../core/service/besoins-auto.service';
import { VehiculeAuto, AchatAuto, UsageAutoSaisie, ConducteurAuto } from '../../core/model/devis-auto';

@Component({ template: '' })
class StubPage {}

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

describe('DevisAutoBesoins', () => {
  let fixture: ComponentFixture<DevisAutoBesoins>;
  let component: DevisAutoBesoins;
  let devisAutoService: DevisAutoService;
  const enregistrerFn = vi.fn();
  const besoinsServiceMock = { enregistrer: enregistrerFn };

  const creer = async () => {
    enregistrerFn.mockClear();
    await TestBed.configureTestingModule({
      imports: [DevisAutoBesoins, StubPage],
      providers: [
        provideRouter([{ path: 'devis-auto/formule', component: StubPage }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: BesoinsAutoService, useValue: besoinsServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DevisAutoBesoins);
    component = fixture.componentInstance;
    devisAutoService = TestBed.inject(DevisAutoService);

    devisAutoService.vehicule.set(vehicule);
    devisAutoService.achat.set(achat);
    devisAutoService.usage.set(usage);
    devisAutoService.enregistrerConducteur(conducteur);
    devisAutoService.enregistrerDevisAutoId(1);
    devisAutoService.enregistrerTarifEtape1({
      tarifable: true,
      prix: [{ formule: 'TIERS', mensuel: 42 }],
    });

    fixture.detectChanges();
    return fixture;
  };

  beforeEach(() => { enregistrerFn.mockClear(); });

  it('bouton desactive quand le formulaire est vide', async () => {
    await creer();
    const bouton = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(bouton.disabled).toBe(true);
  });

  it('bouton actif quand valeur, indispensable et priorite sont remplis', async () => {
    await creer();
    (component as any).valeurVehicule.set(10000);
    (component as any).vehiculeIndispensable.set(true);
    (component as any).priorite.set('EQUILIBRE');
    fixture.detectChanges();
    const bouton = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(bouton.disabled).toBe(false);
  });

  it('affiche 3 tuiles de priorite, aucune active au depart', async () => {
    await creer();
    const tuiles = Array.from(
      fixture.nativeElement.querySelectorAll('.choix--3col .choix__option') as NodeListOf<HTMLElement>
    );
    expect(tuiles.length).toBe(3);
    expect(tuiles.every(t => !t.classList.contains('choix__option--actif'))).toBe(true);
  });

  it('clic EQUILIBRE active la bonne tuile', async () => {
    await creer();
    const tuiles = Array.from(
      fixture.nativeElement.querySelectorAll('.choix--3col .choix__option') as NodeListOf<HTMLButtonElement>
    );
    tuiles[1].click();
    fixture.detectChanges();
    expect((component as any).priorite()).toBe('EQUILIBRE');
    expect(tuiles[1].classList.contains('choix__option--actif')).toBe(true);
  });

  it('clic PROTECTION_LARGE active la bonne tuile', async () => {
    await creer();
    const tuiles = Array.from(
      fixture.nativeElement.querySelectorAll('.choix--3col .choix__option') as NodeListOf<HTMLButtonElement>
    );
    tuiles[2].click();
    fixture.detectChanges();
    expect((component as any).priorite()).toBe('PROTECTION_LARGE');
    expect(tuiles[2].classList.contains('choix__option--actif')).toBe(true);
  });

  it('aside affiche le tarif de tarifEtape1', async () => {
    await creer();
    const aside = fixture.nativeElement.querySelector('.encart-tarif') as HTMLElement;
    expect(aside.textContent).toContain('42');
    expect(aside.textContent).toContain('mois');
  });

  it("aside n'affiche pas de formule conseillee", async () => {
    await creer();
    const aside = fixture.nativeElement.querySelector('.encart-tarif') as HTMLElement;
    expect(aside.textContent).not.toContain('Tous risques');
    expect(aside.textContent).not.toContain('Intermédiaire');
    expect(aside.textContent).not.toContain('Tiers');
  });

  it('submit valide appelle le service besoins avec les bonnes valeurs', async () => {
    await creer();
    enregistrerFn.mockReturnValue(of({ formuleConseillee: 'INTERMEDIAIRE', raisons: ['Equilibre.'], prixMensuel: 38 }));

    (component as any).valeurVehicule.set(10000);
    (component as any).vehiculeIndispensable.set(false);
    (component as any).protectionConducteur.set(false);
    (component as any).priorite.set('EQUILIBRE');
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(enregistrerFn).toHaveBeenCalledWith(1, expect.objectContaining({
      valeurVehicule: 10000,
      vehiculeIndispensable: false,
      priorite: 'EQUILIBRE',
    }));
  });

  it('submit valide : garanties non cochees stockees false', async () => {
    await creer();
    enregistrerFn.mockReturnValue(of({ formuleConseillee: 'INTERMEDIAIRE', raisons: [], prixMensuel: null }));

    (component as any).valeurVehicule.set(5000);
    (component as any).vehiculeIndispensable.set(false);
    (component as any).priorite.set('PRIX');
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(enregistrerFn).toHaveBeenCalledWith(1, expect.objectContaining({
      assistance0km: false,
      protectionConducteur: false,
      brisGlace: false,
      accompagnementLitige: false,
    }));
  });

  it('erreur serveur affiche une alerte', async () => {
    await creer();
    enregistrerFn.mockReturnValue(throwError(() => new Error('erreur reseau')));

    (component as any).valeurVehicule.set(10000);
    (component as any).vehiculeIndispensable.set(false);
    (component as any).priorite.set('PRIX');
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const alerte = fixture.nativeElement.querySelector('.alerte-bloquante');
    expect(alerte).toBeTruthy();
  });
});
