import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LIBELLES_PRODUIT, dateFrancaise } from '../../core/model/contrat';
import { ADRESSE_DECLARER, SinistreResume, jourFrancais, lienReprise } from '../../core/model/sinistre';
import { SinistreService } from '../../core/service/sinistre.service';

/**
 * US-24 : « Mes sinistres », dans le cadre commun (barre latérale). Dossiers déclarés et brouillons pas encore
 * expirés ; un brouillon se reprend à son étape (AC8, RG6 : 30 jours) ; un dossier dont des pièces obligatoires
 * manquent peut être complété (AC7).
 */
@Component({
  selector: 'iard-mes-sinistres',
  imports: [RouterLink],
  styleUrls: ['../mes-devis/mes-devis.scss', '../sinistre-commun/sinistre.scss'],
  templateUrl: './mes-sinistres.html',
})
export class MesSinistres {
  private readonly sinistres = inject(SinistreService);

  protected readonly liste = signal<SinistreResume[] | null>(null);
  protected readonly erreur = signal<string | null>(null);
  /** Message passé par l'étape quittée (« Enregistrer et terminer plus tard »). */
  protected readonly message = signal<string | null>(null);

  protected readonly ADRESSE_DECLARER = ADRESSE_DECLARER;
  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly jourFrancais = jourFrancais;
  protected readonly dateFrancaise = dateFrancaise;
  protected readonly lienReprise = lienReprise;

  constructor() {
    const etat = inject(Router).currentNavigation()?.extras.state as { message?: string } | undefined;
    this.message.set(etat?.message ?? null);
    this.sinistres.lister().subscribe((resultat) => {
      if (resultat.statut === 'ok') {
        this.liste.set(resultat.valeur);
      } else {
        this.erreur.set(resultat.message);
      }
    });
  }
}
