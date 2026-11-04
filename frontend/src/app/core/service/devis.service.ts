import { Injectable, signal } from '@angular/core';
import { DevisLogement, StatutLogement, TypeLogement, logementVide } from '../model/devis';

@Injectable({ providedIn: 'root' })
export class DevisService {
  readonly logement = signal<DevisLogement>(logementVide());

  mettreAJour(partiel: Partial<DevisLogement>): void {
    this.logement.update((actuel) => ({ ...actuel, ...partiel }));
  }

  // PROVISOIRE : 'proprietaire' de la landing est mappé sur 'proprietaire-occupant' par défaut
  prefillDepuisLanding(
    statut: 'locataire' | 'proprietaire',
    typeLogement: TypeLogement,
    codePostal: string,
  ): void {
    const statutDevis: StatutLogement = statut === 'locataire' ? 'locataire' : 'proprietaire-occupant';
    this.mettreAJour({ statut: statutDevis, typeLogement, codePostal });
  }
}
