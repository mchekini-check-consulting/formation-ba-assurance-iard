import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Contrat, LIBELLES_STATUT } from '../../core/model/contrat';
import { formaterDate } from '../../core/model/devis-enregistre';
import { ibanMasqueDepuisFin } from '../../core/model/iban';
import { formaterPrix } from '../../core/model/recommandation';
import { ContratService } from '../../core/service/contrat.service';

/** US-04 : un contrat du client. Le contrat d'un autre client est refusé par le serveur (AC2). */
@Component({
  selector: 'iard-contrat-client',
  imports: [RouterLink],
  styleUrls: ['../compte-cree/compte-cree.scss', '../mes-devis/mes-devis.scss'],
  templateUrl: './contrat-client.html',
})
export class ContratClient {
  protected readonly contrat = signal<Contrat | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly LIBELLES_STATUT = LIBELLES_STATUT;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;
  protected readonly iban = ibanMasqueDepuisFin;

  constructor() {
    const reference = inject(ActivatedRoute).snapshot.paramMap.get('reference') ?? '';
    inject(ContratService)
      .obtenir(reference)
      .subscribe((resultat) => {
        if (resultat.statut === 'obtenu') {
          this.contrat.set(resultat.contrat);
        } else if (resultat.statut === 'introuvable') {
          this.erreur.set('Ce contrat est introuvable.');
        } else {
          this.erreur.set("Ce contrat n'a pas pu être chargé. Réessayez dans quelques instants.");
        }
      });
  }
}
