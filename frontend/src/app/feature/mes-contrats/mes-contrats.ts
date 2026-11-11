import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContratResume, LIBELLES_STATUT } from '../../core/model/contrat';
import { formaterDate } from '../../core/model/devis-enregistre';
import { formaterPrix } from '../../core/model/recommandation';
import { ContratService } from '../../core/service/contrat.service';

/** US-04, AC1 : les contrats du client connecté, et seulement les siens. */
@Component({
  selector: 'iard-mes-contrats',
  imports: [RouterLink],
  styleUrls: ['../compte-cree/compte-cree.scss', '../mes-devis/mes-devis.scss'],
  templateUrl: './mes-contrats.html',
})
export class MesContrats {
  protected readonly liste = signal<ContratResume[] | null>(null);
  protected readonly chargement = signal(true);
  protected readonly LIBELLES_STATUT = LIBELLES_STATUT;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;

  constructor() {
    inject(ContratService)
      .lister()
      .subscribe((liste) => {
        this.chargement.set(false);
        this.liste.set(liste);
      });
  }
}
