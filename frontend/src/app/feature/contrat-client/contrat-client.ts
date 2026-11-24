import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ANCRE_VERSIONS, raccourcisDuContrat } from '../../core/config/raccourcis';
import {
  DocumentContrat,
  FicheContrat,
  LIBELLES_PRODUIT,
  LIBELLES_STATUT_VERSION,
  Regeneration,
  VersionDocument,
  libelleFormule,
  libelleGarantie,
} from '../../core/model/contrat';
import { formaterDate } from '../../core/model/devis-enregistre';
import { formaterPrix } from '../../core/model/recommandation';
import { decrireStatut } from '../../core/model/statut-contrat';
import { ContratService } from '../../core/service/contrat.service';
import {
  DocumentsContratService,
  MESSAGE_EN_PREPARATION,
  MESSAGE_ERREUR_TELECHARGEMENT,
  enregistrerFichier,
} from '../../core/service/documents-contrat.service';
import { BadgeStatut } from '../badge-statut/badge-statut';

/** Ancienne adresse de la fiche (US-04) : la référence du devis de souscription, un UUID. */
const REFERENCE_DEVIS = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * US-18, AC3 : fiche d'un contrat habitation ou auto (garanties, bien assuré, statut, documents), par son numéro.
 * L'ancienne adresse par référence de devis redirige vers la fiche par numéro du même contrat, après le contrôle
 * d'accès du serveur. Le contrat d'un autre client est refusé par le serveur (AC7).
 */
@Component({
  selector: 'iard-contrat-client',
  imports: [RouterLink, BadgeStatut],
  styleUrls: ['../mes-devis/mes-devis.scss', './contrat-client.scss'],
  templateUrl: './contrat-client.html',
})
export class ContratClient {
  private readonly contrats = inject(ContratService);
  private readonly documents = inject(DocumentsContratService);
  private readonly router = inject(Router);

  protected readonly fiche = signal<FicheContrat | null>(null);
  protected readonly erreur = signal<string | null>(null);
  /** Message de téléchargement par document. */
  protected readonly messages = signal<Record<number, string>>({});

  protected readonly statut = computed(() => {
    const fiche = this.fiche();
    return fiche ? decrireStatut(fiche.statut) : null;
  });
  /**
   * AC2 : raccourcis de la fiche. « Voir mon contrat » n'a pas lieu d'être ici, et la régénération a son bouton dans
   * la section « Versions » (US-21).
   */
  protected readonly raccourcis = computed(() => {
    const fiche = this.fiche();
    if (!fiche) return [];
    return raccourcisDuContrat(fiche).filter(
      (raccourci) => raccourci.action !== 'detail' && raccourci.action !== 'regenerer',
    );
  });
  /** US-21, RG2 : régénération possible seulement si le réglage la dit livrée et si le statut la permet (RG3). */
  protected readonly peutRegenerer = computed(() => {
    const fiche = this.fiche();
    return !!fiche && raccourcisDuContrat(fiche).some((raccourci) => raccourci.action === 'regenerer');
  });
  protected readonly regenerationEnCours = signal(false);
  /** US-21, AC3 : message après une demande de régénération. */
  protected readonly messageRegeneration = signal<{ succes: boolean; texte: string } | null>(null);
  protected readonly LIBELLES_STATUT_VERSION = LIBELLES_STATUT_VERSION;

  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly libelleFormule = libelleFormule;
  protected readonly libelleGarantie = libelleGarantie;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;

  private readonly route = inject(ActivatedRoute);

  constructor() {
    this.route.paramMap
      .pipe(takeUntilDestroyed())
      .subscribe((parametres) => this.ouvrir(parametres.get('numero') ?? ''));
  }

  private ouvrir(identifiant: string): void {
    this.fiche.set(null);
    this.erreur.set(null);
    this.messages.set({});
    this.messageRegeneration.set(null);
    if (REFERENCE_DEVIS.test(identifiant)) {
      this.redirigerVersLeNumero(identifiant);
      return;
    }
    this.charger(identifiant, true);
  }

  /** Lit la fiche ; à l'ouverture depuis le raccourci « Régénérer mon contrat », affiche la section « Versions ». */
  private charger(numero: string, ouverture: boolean): void {
    this.contrats.obtenirFiche(numero).subscribe((resultat) => {
      if (resultat.statut === 'obtenu') {
        this.fiche.set(resultat.fiche);
        if (ouverture && this.route.snapshot?.fragment === ANCRE_VERSIONS) {
          setTimeout(() => document.getElementById(ANCRE_VERSIONS)?.scrollIntoView?.());
        }
      } else if (resultat.statut === 'introuvable') {
        this.erreur.set('Ce contrat est introuvable.');
      } else {
        this.erreur.set("Ce contrat n'a pas pu être chargé. Réessayez dans quelques instants.");
      }
    });
  }

  /** Compatibilité : /espace-client/contrats/<référence du devis> → /espace-client/contrats/<numéro>. */
  private redirigerVersLeNumero(referenceDevis: string): void {
    this.contrats.obtenir(referenceDevis).subscribe((resultat) => {
      if (resultat.statut === 'obtenu') {
        void this.router.navigate(['/espace-client/contrats', resultat.contrat.numero], { replaceUrl: true });
      } else if (resultat.statut === 'introuvable') {
        this.erreur.set('Ce contrat est introuvable.');
      } else {
        this.erreur.set("Ce contrat n'a pas pu être chargé. Réessayez dans quelques instants.");
      }
    });
  }

  /** US-21, AC1 et AC3 : régénère les conditions particulières, confirme, puis relit les versions (AC2). */
  protected regenerer(): void {
    const fiche = this.fiche();
    if (!fiche || this.regenerationEnCours()) return;
    this.regenerationEnCours.set(true);
    this.messageRegeneration.set(null);
    this.contrats.regenerer(fiche.numero).subscribe((resultat) => {
      this.regenerationEnCours.set(false);
      if (resultat.statut === 'regenere') {
        this.messageRegeneration.set({ succes: true, texte: messageDeConfirmation(resultat.regeneration) });
        this.charger(fiche.numero, false);
      } else if (resultat.statut === 'trop-tot' || resultat.statut === 'impossible') {
        this.messageRegeneration.set({ succes: false, texte: resultat.message });
      } else {
        this.messageRegeneration.set({
          succes: false,
          texte: "La régénération n'a pas pu aboutir. Réessayez dans quelques instants.",
        });
      }
    });
  }

  /** AC2 : chaque version (en vigueur ou remplacée) se télécharge par la route de T-03. */
  protected telechargerVersion(version: VersionDocument): void {
    this.telecharger({
      id: version.id,
      type: 'CONDITIONS_PARTICULIERES',
      libelle: 'Conditions particulières',
      version: version.version,
      dateCreation: version.dateCreation,
    });
  }

  protected dateDuJour(instant: string | null): string {
    return instant ? new Date(instant).toLocaleDateString('fr-FR', { timeZone: 'Europe/Paris' }) : '';
  }

  protected telecharger(document: DocumentContrat): void {
    const fiche = this.fiche();
    this.messages.update((messages) => ({ ...messages, [document.id]: '' }));
    this.documents.telecharger(document.id).subscribe((resultat) => {
      if (resultat.statut === 'telecharge') {
        enregistrerFichier(resultat.contenu, nomDuFichier(document, fiche?.numero ?? ''));
        return;
      }
      const message = resultat.statut === 'en-preparation' ? MESSAGE_EN_PREPARATION : MESSAGE_ERREUR_TELECHARGEMENT;
      this.messages.update((messages) => ({ ...messages, [document.id]: message }));
    });
  }
}

/** US-21, AC3 : confirmation, selon que le PDF est prêt et que l'e-mail est parti. */
export function messageDeConfirmation(regeneration: Regeneration): string {
  const debut = `Vos conditions particulières sont régénérées (version ${regeneration.version}).`;
  if (!regeneration.disponible) {
    return `${debut} Le document est en préparation : il sera téléchargeable dans quelques minutes.`;
  }
  if (regeneration.emailEnvoye) {
    return `${debut} Nous vous les avons aussi envoyées par e-mail.`;
  }
  return `${debut} L'envoi par e-mail n'a pas abouti : téléchargez-les ci-dessous.`;
}

function nomDuFichier(document: DocumentContrat, numero: string): string {
  const type = document.libelle
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase();
  return `${type}-${numero}-v${document.version}.pdf`;
}
