import { Component, computed, input } from '@angular/core';
import { Produit } from '../../core/model/contrat';
import { lienTelephone, proposeDepannage } from '../../core/model/sinistre';

/**
 * US-24, AC10 : en haut de chaque étape, le numéro d'assistance 24h/24 ; pour un accident auto, « Demander un
 * dépannage » (appel du même numéro en attendant l'écran d'assistance d'US-47) ; en habitation, les gestes d'urgence
 * du type de sinistre.
 */
@Component({
  selector: 'iard-bandeau-urgence',
  styleUrl: './sinistre.scss',
  template: `
    <aside class="urgence" aria-label="Urgence et assistance">
      <p class="urgence__numero">
        Besoin d’aide immédiate ? Assistance 24h/24 :
        <a [href]="lienAppel()"><strong>{{ numero() }}</strong></a>
      </p>
      @if (depannage()) {
        <a class="btn urgence__depannage" [href]="lienAppel()">Demander un dépannage</a>
      }
      @if (produit() === 'HABITATION' && gestes().length > 0) {
        <div class="urgence__gestes">
          <p><strong>Les bons gestes, tout de suite :</strong></p>
          <ul>
            @for (geste of gestes(); track geste) {
              <li>{{ geste }}</li>
            }
          </ul>
        </div>
      }
    </aside>
  `,
})
export class BandeauUrgence {
  readonly numero = input.required<string>();
  readonly produit = input<Produit | null>(null);
  readonly type = input<string | null>(null);
  readonly gestes = input<string[]>([]);

  protected readonly lienAppel = computed(() => lienTelephone(this.numero()));
  protected readonly depannage = computed(() => {
    const produit = this.produit();
    return !!produit && proposeDepannage(produit, this.type());
  });
}
