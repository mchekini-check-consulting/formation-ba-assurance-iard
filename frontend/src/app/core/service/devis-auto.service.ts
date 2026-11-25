import { Injectable, signal } from '@angular/core';
import { AchatAuto, BesoinsAuto, ConducteurAuto, RecommandationAuto, UsageAutoSaisie, VehiculeAuto } from '../model/devis-auto';
import { TarifAutoReponse } from './tarif-auto.service';

@Injectable({ providedIn: 'root' })
export class DevisAutoService {
  readonly vehicule = signal<VehiculeAuto | null>(null);
  readonly achat = signal<AchatAuto | null>(null);
  readonly usage = signal<UsageAutoSaisie | null>(null);
  readonly conducteur = signal<ConducteurAuto | null>(null);
  readonly devisAutoId = signal<number | null>(null);
  readonly besoins = signal<BesoinsAuto | null>(null);
  readonly recommandation = signal<RecommandationAuto | null>(null);
  readonly tarifEtape1 = signal<TarifAutoReponse | null>(null);

  enregistrerVehicule(vehicule: VehiculeAuto): void {
    this.vehicule.set(vehicule);
    this.achat.set(null);
    this.usage.set(null);
    this.conducteur.set(null);
    this.devisAutoId.set(null);
    this.besoins.set(null);
    this.recommandation.set(null);
    this.tarifEtape1.set(null);
  }

  enregistrerAchat(achat: AchatAuto): void {
    this.achat.set(achat);
    this.usage.set(null);
    this.conducteur.set(null);
    this.devisAutoId.set(null);
    this.besoins.set(null);
    this.recommandation.set(null);
    this.tarifEtape1.set(null);
  }

  enregistrerUsage(usage: UsageAutoSaisie): void {
    this.usage.set(usage);
    this.conducteur.set(null);
    this.devisAutoId.set(null);
    this.besoins.set(null);
    this.recommandation.set(null);
    this.tarifEtape1.set(null);
  }

  enregistrerConducteur(conducteur: ConducteurAuto): void {
    this.conducteur.set(conducteur);
    this.besoins.set(null);
    this.recommandation.set(null);
  }

  enregistrerDevisAutoId(id: number): void {
    this.devisAutoId.set(id);
  }

  enregistrerBesoinsEtRecommandation(besoins: BesoinsAuto, recommandation: RecommandationAuto): void {
    this.besoins.set(besoins);
    this.recommandation.set(recommandation);
  }

  enregistrerTarifEtape1(tarif: TarifAutoReponse | null): void {
    this.tarifEtape1.set(tarif);
  }
}
