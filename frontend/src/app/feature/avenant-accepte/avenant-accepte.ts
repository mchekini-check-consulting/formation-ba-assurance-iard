import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ADRESSES } from '../../core/config/raccourcis';
import { Avenant, AvenantAccepte as Acceptation, dateHeureFrancaise, dateJjMmAaaa, montantTtc } from '../../core/model/avenant';
import { FicheContrat, LIBELLES_PRODUIT, libelleGarantie } from '../../core/model/contrat';
import { ContratService } from '../../core/service/contrat.service';
import {
  DocumentsContratService,
  MESSAGE_EN_PREPARATION,
  MESSAGE_ERREUR_TELECHARGEMENT,
  enregistrerFichier,
} from '../../core/service/documents-contrat.service';

/**
 * US-23, AC4 et AC7 : « Avenant accepté ». Confirme l'acceptation (date et heure), la date d'effet, la nouvelle
 * cotisation et les nouvelles garanties, l'envoi par e-mail et la nouvelle version des conditions particulières ;
 * l'avenant se télécharge. Les données viennent de la fiche du contrat, relue sans recharger la page.
 */
@Component({
  selector: 'iard-avenant-accepte',
  imports: [RouterLink],
  styleUrls: [
    '../mes-devis/mes-devis.scss',
    '../contrat-client/contrat-client.scss',
    '../modifier-garanties/modifier-garanties.scss',
  ],
  templateUrl: './avenant-accepte.html',
})
export class AvenantAccepte {
  private readonly contrats = inject(ContratService);
  private readonly documents = inject(DocumentsContratService);
  private readonly route = inject(ActivatedRoute);
  /** Résultat de l'acceptation, transmis par « Relire et accepter l'avenant » (absent si la page est rouverte). */
  private readonly acceptation: Acceptation | null =
    (inject(Router).currentNavigation()?.extras.state?.['acceptation'] as Acceptation | undefined) ?? null;

  protected readonly numero = signal('');
  protected readonly fiche = signal<FicheContrat | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly messageTelechargement = signal<string | null>(null);

  /** Le dernier avenant accepté du contrat. */
  protected readonly avenant = computed<Avenant | null>(
    () => this.fiche()?.avenants?.find((avenant) => avenant.statut === 'ACCEPTE') ?? null,
  );
  protected readonly lienFiche = computed(() => [ADRESSES.modifierGaranties ?? '/espace-client/contrats', this.numero()]);
  protected readonly messageEnvoi = computed(() => {
    if (!this.acceptation) return null;
    if (!this.acceptation.disponible) {
      return 'Votre avenant est en préparation : vous le recevrez par e-mail et pourrez le télécharger dans quelques minutes.';
    }
    return this.acceptation.emailEnvoye
      ? 'Nous vous avons envoyé votre avenant par e-mail.'
      : "L'envoi par e-mail n'a pas abouti : téléchargez votre avenant ci-dessous.";
  });
  protected readonly versionConditionsParticulieres = computed(
    () => this.fiche()?.versionsConditionsParticulieres.find((version) => version.statut === 'EN_VIGUEUR')?.version ?? null,
  );

  protected readonly montantTtc = montantTtc;
  protected readonly dateJjMmAaaa = dateJjMmAaaa;
  protected readonly dateHeureFrancaise = dateHeureFrancaise;
  protected readonly libelleGarantie = libelleGarantie;
  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((parametres) => {
      this.numero.set(parametres.get('numero') ?? '');
      this.contrats.obtenirFiche(this.numero()).subscribe((resultat) => {
        if (resultat.statut === 'obtenu') {
          this.fiche.set(resultat.fiche);
        } else {
          this.erreur.set(
            resultat.statut === 'introuvable'
              ? 'Ce contrat est introuvable.'
              : "Votre contrat n'a pas pu être chargé. Réessayez dans quelques instants.",
          );
        }
      });
    });
  }

  protected telecharger(avenant: Avenant): void {
    if (avenant.documentId === null) return;
    this.messageTelechargement.set(null);
    this.documents.telecharger(avenant.documentId).subscribe((resultat) => {
      if (resultat.statut === 'telecharge') {
        enregistrerFichier(resultat.contenu, `avenant-${avenant.numero}.pdf`);
        return;
      }
      this.messageTelechargement.set(
        resultat.statut === 'en-preparation' ? MESSAGE_EN_PREPARATION : MESSAGE_ERREUR_TELECHARGEMENT,
      );
    });
  }
}
