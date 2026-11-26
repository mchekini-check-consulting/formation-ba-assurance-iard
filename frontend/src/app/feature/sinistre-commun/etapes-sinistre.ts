import { Component, input } from '@angular/core';

/** US-24 : « 1 Que s'est-il passé ? · 2 Circonstances · 3 Justificatifs et envoi », en haut de chaque étape. */
@Component({
  selector: 'iard-etapes-sinistre',
  styleUrl: './sinistre.scss',
  template: `
    <ol class="etapes" aria-label="Étapes de la déclaration">
      @for (libelle of libelles; track libelle; let i = $index) {
        <li
          class="etapes__etape"
          [class.etapes__etape--active]="i + 1 === etape()"
          [class.etapes__etape--faite]="i + 1 < etape()"
          [attr.aria-current]="i + 1 === etape() ? 'step' : null"
        >
          <span class="etapes__numero">{{ i + 1 }}</span> {{ libelle }}
        </li>
      }
    </ol>
  `,
})
export class EtapesSinistre {
  readonly etape = input.required<number>();
  protected readonly libelles = ['Que s’est-il passé ?', 'Circonstances', 'Justificatifs et envoi'];
}
