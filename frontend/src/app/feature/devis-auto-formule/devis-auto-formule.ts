import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DevisAutoService } from '../../core/service/devis-auto.service';

@Component({
  selector: 'iard-devis-auto-formule',
  imports: [RouterLink],
  templateUrl: './devis-auto-formule.html',
})
export class DevisAutoFormule {
  protected readonly devisAutoService = inject(DevisAutoService);

  protected formaterPrix(prix: number): string {
    return prix.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected libelle(formule: string): string {
    switch (formule) {
      case 'TOUS_RISQUES': return 'Tous risques';
      case 'INTERMEDIAIRE': return 'Intermédiaire';
      default: return 'Tiers';
    }
  }
}
