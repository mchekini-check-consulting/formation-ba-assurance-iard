import { Component, computed, input } from '@angular/core';
import { decrireStatut } from '../../core/model/statut-contrat';

/** US-18, AC5 : badge du statut d'un contrat. Le nom du statut est toujours écrit ; la couleur ne fait qu'accompagner. */
@Component({
  selector: 'iard-badge-statut',
  template: `<span class="badge-statut badge-statut--{{ description().ton }}">{{ description().libelle }}</span>`,
  styleUrl: './badge-statut.scss',
})
export class BadgeStatut {
  readonly statut = input.required<string>();
  protected readonly description = computed(() => decrireStatut(this.statut()));
}
