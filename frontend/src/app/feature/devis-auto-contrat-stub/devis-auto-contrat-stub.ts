import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { DevisAutoLayout } from '../devis-auto-layout/devis-auto-layout';

@Component({
  selector: 'iard-devis-auto-contrat-stub',
  standalone: true,
  imports: [DevisAutoLayout, RouterLink],
  template: `
    <iard-devis-auto-layout sousEtape="6">
      <main id="contenu" style="padding: 48px 24px; max-width: 700px; margin: 0 auto;">
        <h1>Étape 6 – Mon contrat <span style="font-size: 0.7em; color: #888;">(à venir)</span></h1>
        @if (svc.contratGenere(); as contrat) {
          <p>Votre contrat <strong>{{ contrat.numero }}</strong> a bien été créé.</p>
          <p>Statut&nbsp;: {{ contrat.statut }}</p>
        }
        <a routerLink="/devis-auto/paiement" style="color: #a82b27; text-decoration: underline;">← Retour au paiement</a>
      </main>
    </iard-devis-auto-layout>
  `,
})
export class DevisAutoContratStub {
  protected readonly svc = inject(DevisAutoService);
}
