import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DevisAutoService } from '../../core/service/devis-auto.service';

@Component({
  selector: 'iard-devis-auto-paiement-stub',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main style="padding: 48px 24px; max-width: 700px; margin: 0 auto; font-family: sans-serif;">
      <h1>Étape 5 – Paiement <span style="font-size: 0.7em; color: #888;">(à venir, US-32)</span></h1>
      <p>Devis n°&nbsp;<strong>{{ svc.devisAutoId() }}</strong></p>
      <a routerLink="/devis-auto/informations" style="color: #a82b27; text-decoration: underline;">← Retour à mes informations</a>
    </main>
  `,
})
export class DevisAutoPaiementStub {
  protected readonly svc = inject(DevisAutoService);
}
