import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DevisEnregistre, MENTION_CONSERVATION, formaterDate, messageReprise } from '../../core/model/devis-enregistre';
import { FORMULES, formaterPrix } from '../../core/model/recommandation';
import { DevisEnregistreService } from '../../core/service/devis-enregistre.service';
import { DevisService } from '../../core/service/devis.service';

/** US-12 : les devis du client connecté. */
@Component({
  selector: 'iard-mes-devis',
  imports: [RouterLink],
  styleUrls: ['../compte-cree/compte-cree.scss', './mes-devis.scss'],
  templateUrl: './mes-devis.html',
})
export class MesDevis {
  private readonly service = inject(DevisEnregistreService);
  private readonly devis = inject(DevisService);
  private readonly router = inject(Router);

  protected readonly liste = signal<DevisEnregistre[] | null>(null);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  /** Devis dont la suppression attend une confirmation. */
  protected readonly aConfirmer = signal<string | null>(null);

  protected readonly MENTION_CONSERVATION = MENTION_CONSERVATION;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;

  constructor() {
    this.service.lister().subscribe((liste) => {
      this.chargement.set(false);
      this.liste.set(liste);
      if (liste === null) {
        this.erreur.set("Vos devis n'ont pas pu être chargés. Réessayez dans quelques instants.");
      }
    });
  }

  protected nomFormule(ligne: DevisEnregistre): string {
    return FORMULES[ligne.formule].nom;
  }

  protected reprendre(reference: string): void {
    this.erreur.set(null);
    this.service.reprendre(reference).subscribe((reprise) => {
      if (reprise === null) {
        this.erreur.set("Ce devis n'a pas pu être repris. Réessayez dans quelques instants.");
        return;
      }
      this.devis.restaurer(reprise);
      this.devis.messageReprise.set(messageReprise(reprise));
      void this.router.navigate(['/devis/formule']);
    });
  }

  protected demanderSuppression(reference: string): void {
    this.aConfirmer.set(reference);
  }

  protected annulerSuppression(): void {
    this.aConfirmer.set(null);
  }

  protected supprimer(reference: string): void {
    this.erreur.set(null);
    this.service.supprimer(reference).subscribe((supprime) => {
      this.aConfirmer.set(null);
      if (!supprime) {
        this.erreur.set("Ce devis n'a pas pu être supprimé. Réessayez dans quelques instants.");
        return;
      }
      this.liste.update((liste) => liste?.filter((ligne) => ligne.reference !== reference) ?? null);
    });
  }
}
