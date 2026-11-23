import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DossierClient as Dossier, nomComplet } from '../../core/model/back-office';
import { formaterDate } from '../../core/model/devis-enregistre';
import { libelleCategorie } from '../../core/model/document';
import { ibanMasqueDepuisFin } from '../../core/model/iban';
import { formaterPrix } from '../../core/model/recommandation';
import { BackOfficeService } from '../../core/service/back-office.service';

/** US-04 : dossier d'un client vu du back-office. AC4 : IBAN masqué, seuls les 4 derniers caractères visibles. */
@Component({
  selector: 'iard-dossier-client',
  imports: [RouterLink],
  styleUrls: ['../compte-cree/compte-cree.scss', '../mes-devis/mes-devis.scss'],
  templateUrl: './dossier-client.html',
})
export class DossierClient {
  protected readonly dossier = signal<Dossier | null>(null);
  protected readonly erreur = signal(false);
  protected readonly nomComplet = nomComplet;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;
  protected readonly libelleCategorie = libelleCategorie;
  protected readonly iban = ibanMasqueDepuisFin;

  constructor() {
    const id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';
    inject(BackOfficeService)
      .dossier(id)
      .subscribe((dossier) => {
        this.dossier.set(dossier);
        this.erreur.set(dossier === null);
      });
  }
}
