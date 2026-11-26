import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { formaterNomPropre } from '../../core/model/identite';
import { dateFrancaise } from '../../core/model/contrat';
import { ADRESSE_SINISTRES, DeclarationSinistre, SinistreEnvoye } from '../../core/model/sinistre';
import { SinistreService } from '../../core/service/sinistre.service';

/** US-24, AC9 : prochaines étapes affichées après l'envoi (maquette « Déclaration envoyée »). */
export const PROCHAINES_ETAPES = [
  'Votre dossier est transmis au service sinistres, qui l’étudie.',
  'Un gestionnaire vous contacte si des informations ou des pièces manquent.',
  'Vous suivez l’avancement de votre dossier dans « Mes sinistres ».',
];

/**
 * US-24, AC9 : « Déclaration envoyée ». Numéro de dossier SIN-AAAA-XXXXXX, statut Déclaré, prochaines étapes et
 * accusé de réception par e-mail ; rappel des justificatifs obligatoires qui manquent encore (AC7).
 */
@Component({
  selector: 'iard-sinistre-envoye',
  imports: [RouterLink],
  styleUrls: ['../mes-devis/mes-devis.scss', '../sinistre-commun/sinistre.scss'],
  templateUrl: './sinistre-envoye.html',
})
export class SinistreEnvoyePage {
  private readonly sinistres = inject(SinistreService);
  private readonly route = inject(ActivatedRoute);

  protected readonly declaration = signal<DeclarationSinistre | null>(null);
  protected readonly erreur = signal<string | null>(null);
  /** Résultat de l'envoi (passé par l'étape 3) : l'accusé de réception est-il parti ? */
  protected readonly envoi = signal<SinistreEnvoye | null>(null);

  protected readonly ADRESSE_SINISTRES = ADRESSE_SINISTRES;
  protected readonly PROCHAINES_ETAPES = PROCHAINES_ETAPES;
  protected readonly formaterNomPropre = formaterNomPropre;
  protected readonly dateFrancaise = dateFrancaise;

  constructor() {
    const etat = inject(Router).currentNavigation()?.extras.state as { envoi?: SinistreEnvoye } | undefined;
    this.envoi.set(etat?.envoi ?? null);
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((parametres) => {
      this.sinistres.lire(Number(parametres.get('id'))).subscribe((resultat) => {
        if (resultat.statut === 'ok') {
          this.declaration.set(resultat.valeur);
        } else {
          this.erreur.set(resultat.message);
        }
      });
    });
  }
}
