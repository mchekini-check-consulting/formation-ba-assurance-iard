import { Injectable, signal } from '@angular/core';
import { AchatAuto, ConducteurAuto, UsageAutoSaisie, VehiculeAuto } from '../model/devis-auto';

@Injectable({ providedIn: 'root' })
export class DevisAutoService {
  readonly vehicule = signal<VehiculeAuto | null>(null);
  readonly achat = signal<AchatAuto | null>(null);
  readonly usage = signal<UsageAutoSaisie | null>(null);
  readonly conducteur = signal<ConducteurAuto | null>(null);

  enregistrerVehicule(vehicule: VehiculeAuto): void {
    this.vehicule.set(vehicule);
    this.achat.set(null);
    this.usage.set(null);
    this.conducteur.set(null);
  }

  enregistrerAchat(achat: AchatAuto): void {
    this.achat.set(achat);
    this.usage.set(null);
    this.conducteur.set(null);
  }

  enregistrerUsage(usage: UsageAutoSaisie): void {
    this.usage.set(usage);
    this.conducteur.set(null);
  }

  enregistrerConducteur(conducteur: ConducteurAuto): void {
    this.conducteur.set(conducteur);
  }
}
