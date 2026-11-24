import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { raccourcisDuContrat } from '../../core/config/raccourcis';
import { ContratResume, LIBELLES_PRODUIT } from '../../core/model/contrat';
import { formaterDate } from '../../core/model/devis-enregistre';
import { formaterPrix } from '../../core/model/recommandation';
import { decrireStatut } from '../../core/model/statut-contrat';
import { BadgeStatut } from '../badge-statut/badge-statut';

/** US-18 : un contrat (numéro, statut, prime, échéance) et ses raccourcis, sur le tableau de bord et dans Mes contrats. */
@Component({
  selector: 'iard-carte-contrat',
  imports: [RouterLink, BadgeStatut],
  templateUrl: './carte-contrat.html',
  styleUrl: '../mes-devis/mes-devis.scss',
})
export class CarteContrat {
  readonly contrat = input.required<ContratResume>();

  protected readonly message = computed(() => decrireStatut(this.contrat().statut).message);
  protected readonly raccourcis = computed(() => raccourcisDuContrat(this.contrat()));
  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;
}
