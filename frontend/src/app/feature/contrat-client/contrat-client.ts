import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { raccourcisDuContrat } from '../../core/config/raccourcis';
import { DocumentContrat, FicheContrat, LIBELLES_PRODUIT, libelleFormule, libelleGarantie } from '../../core/model/contrat';
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
  /** AC2 : raccourcis de la fiche (« Voir le détail » n'a pas lieu d'être ici). */
  protected readonly raccourcis = computed(() => {
    const fiche = this.fiche();
    if (!fiche) return [];
    return raccourcisDuContrat(fiche).filter((raccourci) => raccourci.action !== 'detail');
  });

  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly libelleFormule = libelleFormule;
  protected readonly libelleGarantie = libelleGarantie;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((parametres) => this.ouvrir(parametres.get('numero') ?? ''));
  }

  private ouvrir(identifiant: string): void {
    this.fiche.set(null);
    this.erreur.set(null);
    this.messages.set({});
    if (REFERENCE_DEVIS.test(identifiant)) {
      this.redirigerVersLeNumero(identifiant);
      return;
    }
    this.contrats.obtenirFiche(identifiant).subscribe((resultat) => {
      if (resultat.statut === 'obtenu') {
        this.fiche.set(resultat.fiche);
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

function nomDuFichier(document: DocumentContrat, numero: string): string {
  const type = document.libelle
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase();
  return `${type}-${numero}-v${document.version}.pdf`;
}
