import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { LIBELLES_PRODUIT } from '../../core/model/contrat';
import { formaterDate } from '../../core/model/devis-enregistre';
import {
  ContratFactures,
  FactureClient,
  LIBELLES_STATUT_ECHEANCE,
  MESSAGE_AUCUNE_COTISATION,
  MesFactures as DonneesFactures,
  PeriodeFacture,
} from '../../core/model/facture';
import { formaterPrix } from '../../core/model/recommandation';
import { ConnexionService } from '../../core/service/connexion.service';
import {
  DocumentsContratService,
  MESSAGE_EN_PREPARATION,
  MESSAGE_ERREUR_TELECHARGEMENT,
  enregistrerFichier,
} from '../../core/service/documents-contrat.service';
import { FacturesService, MESSAGE_ERREUR_FACTURE } from '../../core/service/factures.service';
import { BadgeStatut } from '../badge-statut/badge-statut';

/** Message affiché sous un contrat (génération) ou sous une facture (téléchargement). */
export interface Message {
  succes: boolean;
  texte: string;
}

/** Ancre d'un contrat dans la page (raccourci « Générer une facture » des contrats). */
export function ancreContrat(numero: string): string {
  return `contrat-${numero}`;
}

/** Message après « Générer une facture ». */
export function messageGeneration(numero: string, existante: boolean, disponible: boolean): string {
  const debut = existante
    ? `Cette facture a déjà été générée : ${numero}.`
    : `Votre facture ${numero} est générée.`;
  return disponible ? `${debut} Son téléchargement commence.` : `${debut} ${MESSAGE_EN_PREPARATION}`;
}

/**
 * US-22 : « Mes factures ». AC1 : échéancier de chaque contrat (date, montant TTC, statut), du plus récent au plus
 * ancien ; AC2 : « Générer une facture » pour une échéance ou une année de cotisation, puis téléchargement, et la
 * facture apparaît dans la liste ; AC4 : message tant qu'aucune cotisation n'a été émise.
 */
@Component({
  selector: 'iard-mes-factures',
  imports: [BadgeStatut],
  styleUrls: ['../mes-devis/mes-devis.scss', './mes-factures.scss'],
  templateUrl: './mes-factures.html',
})
export class MesFactures {
  private readonly factures = inject(FacturesService);
  private readonly documents = inject(DocumentsContratService);
  private readonly route = inject(ActivatedRoute, { optional: true });

  protected readonly donnees = signal<DonneesFactures | null>(null);
  protected readonly chargement = signal(true);
  /** US-04 : le bouton n'est proposé qu'avec le droit de créer des documents (le serveur le vérifie aussi). */
  protected readonly peutGenerer = inject(ConnexionService).aLeDroit('DOCUMENTS', 'CREER');
  /** Clé de la génération en cours (un seul clic à la fois). */
  protected readonly enCours = signal<string | null>(null);
  /** Message de génération par numéro de contrat. */
  protected readonly messagesContrat = signal<Record<string, Message>>({});
  /** Message de téléchargement par numéro de facture. */
  protected readonly messagesFacture = signal<Record<string, Message>>({});

  protected readonly libellesProduit = LIBELLES_PRODUIT;
  protected readonly libellesStatut = LIBELLES_STATUT_ECHEANCE;
  protected readonly messageAucuneCotisation = MESSAGE_AUCUNE_COTISATION;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;
  protected readonly ancreContrat = ancreContrat;

  constructor() {
    this.factures.lister().subscribe((donnees) => {
      this.donnees.set(donnees);
      this.chargement.set(false);
      // Raccourci « Générer une facture » d'un contrat : la page s'ouvre sur ce contrat.
      const fragment = this.route?.snapshot?.fragment;
      if (fragment) {
        setTimeout(() => document.getElementById(fragment)?.scrollIntoView?.());
      }
    });
  }

  protected cle(contrat: ContratFactures, periode: PeriodeFacture, date: string): string {
    return `${contrat.numero}|${periode}|${date}`;
  }

  protected generer(contrat: ContratFactures, periode: PeriodeFacture, date: string): void {
    if (this.enCours()) return;
    this.enCours.set(this.cle(contrat, periode, date));
    this.messagesContrat.update((messages) => ({ ...messages, [contrat.numero]: { succes: true, texte: '' } }));
    this.factures.generer(contrat.numero, periode, date).subscribe((resultat) => {
      this.enCours.set(null);
      if (resultat.statut === 'genere') {
        const { facture, existante, disponible } = resultat.generation;
        this.ajouter(contrat.numero, facture);
        this.messageContrat(contrat.numero, true, messageGeneration(facture.numero, existante, disponible));
        if (disponible) {
          this.telecharger(facture);
        }
      } else if (resultat.statut === 'erreur-technique') {
        this.messageContrat(contrat.numero, false, MESSAGE_ERREUR_FACTURE);
      } else {
        this.messageContrat(contrat.numero, false, resultat.message);
      }
    });
  }

  protected telecharger(facture: FactureClient): void {
    if (facture.documentId === null) return;
    this.messagesFacture.update((messages) => ({ ...messages, [facture.numero]: { succes: true, texte: '' } }));
    this.documents.telecharger(facture.documentId).subscribe((resultat) => {
      if (resultat.statut === 'telecharge') {
        enregistrerFichier(resultat.contenu, `${facture.numero}.pdf`);
        return;
      }
      const texte = resultat.statut === 'en-preparation' ? MESSAGE_EN_PREPARATION : MESSAGE_ERREUR_TELECHARGEMENT;
      this.messagesFacture.update((messages) => ({ ...messages, [facture.numero]: { succes: false, texte } }));
    });
  }

  /** AC2 : la facture apparaît dans la liste (une seule fois), la plus récente période d'abord. */
  private ajouter(numeroContrat: string, facture: FactureClient): void {
    this.donnees.update((donnees) => {
      if (!donnees) return donnees;
      return {
        aucuneCotisation: false,
        contrats: donnees.contrats.map((contrat) => {
          if (contrat.numero !== numeroContrat) return contrat;
          const autres = contrat.factures.filter((une) => une.numero !== facture.numero);
          const factures = [facture, ...autres].sort((a, b) => b.periodeFin.localeCompare(a.periodeFin));
          return { ...contrat, factures };
        }),
      };
    });
  }

  private messageContrat(numero: string, succes: boolean, texte: string): void {
    this.messagesContrat.update((messages) => ({ ...messages, [numero]: { succes, texte } }));
  }
}
