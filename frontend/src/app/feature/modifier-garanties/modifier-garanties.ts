import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ADRESSES, CHEMIN_AVENANT } from '../../core/config/raccourcis';
import {
  GarantieProposee,
  GarantiesContrat,
  aChange,
  cotisationApres,
  garantiesSouscrites,
  montantTtc,
} from '../../core/model/avenant';
import { LIBELLES_PRODUIT, libelleFormule } from '../../core/model/contrat';
import { formaterNomPropre } from '../../core/model/identite';
import { AvenantService } from '../../core/service/avenant.service';

/**
 * US-23, AC1 et AC2 : « Modifier mes garanties » d'un contrat actif. Garanties actuelles et options ajoutables avec
 * leur prix ; une garantie obligatoire (responsabilité civile en auto) n'est pas modifiable et un message en donne la
 * raison. « Continuer » reste désactivé tant qu'aucune garantie n'a changé ; il prépare l'avenant (AC3) puis ouvre
 * « Relire et accepter l'avenant ». AC6 : un contrat qui n'est pas actif est refusé par le serveur (message).
 */
@Component({
  selector: 'iard-modifier-garanties',
  imports: [RouterLink],
  styleUrls: ['../mes-devis/mes-devis.scss', '../contrat-client/contrat-client.scss', './modifier-garanties.scss'],
  templateUrl: './modifier-garanties.html',
})
export class ModifierGaranties {
  private readonly avenants = inject(AvenantService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly numero = signal('');
  protected readonly contrat = signal<GarantiesContrat | null>(null);
  protected readonly erreur = signal<string | null>(null);
  /** Codes des garanties voulues après l'avenant ; au départ, celles du contrat. */
  protected readonly selection = signal<ReadonlySet<string>>(new Set());
  protected readonly envoiEnCours = signal(false);
  protected readonly messageEnvoi = signal<string | null>(null);

  protected readonly prenom = computed(() => formaterNomPropre(this.contrat()?.prenom));
  protected readonly change = computed(() => {
    const contrat = this.contrat();
    return !!contrat && aChange(contrat.garanties, this.selection());
  });
  protected readonly nouvelleCotisation = computed(() => {
    const contrat = this.contrat();
    return contrat ? cotisationApres(contrat.cotisationMensuelle, contrat.garanties, this.selection()) : 0;
  });
  protected readonly lienFiche = computed(() => [ADRESSES.modifierGaranties ?? '/espace-client/contrats', this.numero()]);
  protected readonly lienAvenant = computed(() => [...this.lienFiche(), CHEMIN_AVENANT]);

  protected readonly montantTtc = montantTtc;
  protected readonly libelleFormule = libelleFormule;
  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((parametres) => {
      this.numero.set(parametres.get('numero') ?? '');
      this.charger();
    });
  }

  private charger(): void {
    this.contrat.set(null);
    this.erreur.set(null);
    this.avenants.garanties(this.numero()).subscribe((resultat) => {
      if (resultat.statut === 'obtenu') {
        this.contrat.set(resultat.garanties);
        this.selection.set(new Set(garantiesSouscrites(resultat.garanties.garanties)));
      } else {
        this.erreur.set(resultat.message);
      }
    });
  }

  protected choisie(garantie: GarantieProposee): boolean {
    return this.selection().has(garantie.code);
  }

  /** AC2 : une garantie non modifiable ne change pas. */
  protected basculer(garantie: GarantieProposee): void {
    if (!garantie.modifiable) return;
    const selection = new Set(this.selection());
    if (selection.has(garantie.code)) {
      selection.delete(garantie.code);
    } else {
      selection.add(garantie.code);
    }
    this.selection.set(selection);
    this.messageEnvoi.set(null);
  }

  /** AC3 : prépare l'avenant, puis ouvre « Relire et accepter l'avenant ». */
  protected continuer(): void {
    if (!this.change() || this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.messageEnvoi.set(null);
    this.avenants.preparer(this.numero(), [...this.selection()]).subscribe((resultat) => {
      this.envoiEnCours.set(false);
      if (resultat.statut === 'prepare') {
        void this.router.navigate(this.lienAvenant());
      } else {
        this.messageEnvoi.set(resultat.message);
      }
    });
  }

  /** « + 2,50 € TTC / mois » pour une option à ajouter, « 2,50 € TTC / mois » pour une garantie souscrite. */
  protected prix(garantie: GarantieProposee): string {
    if (garantie.prixMensuelTtc === null) return '';
    return `${garantie.souscrite ? '' : '+ '}${montantTtc(garantie.prixMensuelTtc)} / mois`;
  }
}
