import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContratResume } from '../../core/model/contrat';
import { ContratService } from '../../core/service/contrat.service';
import { CarteContrat } from '../carte-contrat/carte-contrat';

/** US-04, AC1 / US-18 : les contrats habitation et auto du client connecté, et seulement les siens. */
@Component({
  selector: 'iard-mes-contrats',
  imports: [RouterLink, CarteContrat],
  styleUrls: ['../compte-cree/compte-cree.scss', '../mes-devis/mes-devis.scss'],
  templateUrl: './mes-contrats.html',
})
export class MesContrats {
  protected readonly liste = signal<ContratResume[] | null>(null);
  protected readonly chargement = signal(true);

  constructor() {
    inject(ContratService)
      .lister()
      .subscribe((liste) => {
        this.chargement.set(false);
        this.liste.set(liste);
      });
  }
}
