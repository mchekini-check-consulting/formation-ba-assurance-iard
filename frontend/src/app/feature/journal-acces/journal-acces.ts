import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccesRefuse, LIBELLES_MOTIF } from '../../core/model/back-office';
import { BackOfficeService } from '../../core/service/back-office.service';

/** US-04, RG3 : journal des accès refusés, pour l'administrateur (200 dernières tentatives). */
@Component({
  selector: 'iard-journal-acces',
  imports: [RouterLink],
  styleUrls: ['../compte-cree/compte-cree.scss', '../mes-devis/mes-devis.scss', './journal-acces.scss'],
  templateUrl: './journal-acces.html',
})
export class JournalAcces {
  protected readonly lignes = signal<AccesRefuse[] | null>(null);
  protected readonly chargement = signal(true);

  constructor() {
    inject(BackOfficeService)
      .accesRefuses()
      .subscribe((lignes) => {
        this.chargement.set(false);
        this.lignes.set(lignes);
      });
  }

  protected motif(code: string): string {
    return LIBELLES_MOTIF[code] ?? code;
  }

  protected date(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'medium' });
  }
}
