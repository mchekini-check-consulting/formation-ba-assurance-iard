import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DevisAutoService } from '../../core/service/devis-auto.service';

@Component({
  selector: 'iard-devis-auto-informations-stub',
  imports: [RouterLink, DecimalPipe],
  template: `
    <main style="padding: 48px 24px; max-width: 700px; margin: 0 auto; font-family: sans-serif;">
      <h1>Étape 4 – Mes informations <span style="font-size: 0.7em; color: #888;">(à venir, US-31)</span></h1>
      @if (svc.formuleChoisie(); as f) {
        <p>
          Devis n°&nbsp;<strong>{{ svc.devisAutoId() }}</strong> –
          Formule&nbsp;<strong>{{ f.formuleMoteur }}</strong>
          @if (f.prixMensuelTtc) { – <strong>{{ f.prixMensuelTtc | number: '1.2-2' : 'fr' }}&nbsp;€/mois</strong> }
        </p>
      }
      <a routerLink="/devis-auto/formule" style="color: #a82b27; text-decoration: underline;">← Retour à ma formule</a>
    </main>
  `,
})
export class DevisAutoInformationsStub {
  protected readonly svc = inject(DevisAutoService);
}
