import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClientResume, nomComplet } from '../../core/model/back-office';
import { BackOfficeService } from '../../core/service/back-office.service';

/** US-04 : back-office, liste des clients (Gestionnaire, Administrateur). */
@Component({
  selector: 'iard-dossiers-clients',
  imports: [RouterLink],
  styleUrls: ['../compte-cree/compte-cree.scss', '../mes-devis/mes-devis.scss'],
  templateUrl: './dossiers-clients.html',
})
export class DossiersClients {
  protected readonly liste = signal<ClientResume[] | null>(null);
  protected readonly chargement = signal(true);
  protected readonly nomComplet = nomComplet;

  constructor() {
    inject(BackOfficeService)
      .listerClients()
      .subscribe((liste) => {
        this.chargement.set(false);
        this.liste.set(liste);
      });
  }
}
