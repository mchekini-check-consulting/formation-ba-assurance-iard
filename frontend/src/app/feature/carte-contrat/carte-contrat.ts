import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { raccourcisDuContrat } from '../../core/config/raccourcis';
import { ContratResume, LIBELLES_PRODUIT, libelleFormule } from '../../core/model/contrat';
import { formaterNomPropre } from '../../core/model/identite';
import { formaterPrix } from '../../core/model/recommandation';
import { decrireStatut } from '../../core/model/statut-contrat';
import { BadgeStatut } from '../badge-statut/badge-statut';

/**
 * US-18 : un contrat sur le tableau de bord et dans Mes contrats (maquette) : produit et statut, bien assuré en titre,
 * formule et numéro, prime, message du statut (RG3), « Voir mon contrat » puis les raccourcis livrés (AC2).
 */
@Component({
  selector: 'iard-carte-contrat',
  imports: [RouterLink, BadgeStatut],
  templateUrl: './carte-contrat.html',
  styleUrl: './carte-contrat.scss',
})
export class CarteContrat {
  readonly contrat = input.required<ContratResume>();

  /** « Appartement · Créteil » (habitation) ou « Peugeot 208 · AA-001-AA » (auto). */
  protected readonly titre = computed(() => {
    const c = this.contrat();
    if (c.produit === 'HABITATION' && (c.typeLogement || c.ville)) {
      return [formaterNomPropre(c.typeLogement), c.ville].filter(Boolean).join(' · ');
    }
    return c.bienAssure ?? `Contrat ${c.numero}`;
  });
  protected readonly message = computed(() => decrireStatut(this.contrat().statut).message);
  protected readonly raccourcis = computed(() => raccourcisDuContrat(this.contrat()));
  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly libelleFormule = libelleFormule;
  protected readonly formaterPrix = formaterPrix;
}
