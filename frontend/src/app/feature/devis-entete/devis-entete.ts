import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'iard-devis-entete',
  imports: [RouterLink],
  styleUrls: ['../devis-logement/devis-logement.scss', './devis-entete.scss'],
  templateUrl: './devis-entete.html',
})
export class DevisEntete {
  readonly etape = input.required<number>();

  protected readonly etapes = ['Mon logement', 'Mes besoins', 'Ma formule', 'Mes informations', 'Paiement'];
}
