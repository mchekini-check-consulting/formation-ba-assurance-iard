import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LIBELLES_PRODUIT, libelleFormule } from '../../core/model/contrat';
import {
  ACCEPT_FICHIERS,
  ADRESSE_SINISTRES,
  DeclarationSinistre,
  LIBELLES_DECLARANT,
  MENTION_HONNEUR,
  MESSAGE_BLESSES,
  PieceAttendue,
  PieceJointe,
  TEXTE_HONNEUR,
  heureFrancaise,
  jourFrancais,
  refusFichier,
  reponsesDe,
  taille,
} from '../../core/model/sinistre';
import { SinistreService } from '../../core/service/sinistre.service';
import { MESSAGE_ENREGISTRE } from '../declarer-sinistre/declarer-sinistre';
import { BandeauUrgence } from '../sinistre-commun/bandeau-urgence';
import { EtapesSinistre } from '../sinistre-commun/etapes-sinistre';

export const MESSAGE_HONNEUR = 'Cochez la déclaration sur l’honneur pour envoyer votre déclaration.';
export const MESSAGE_PIECES_PLUS_TARD =
  'Vous pouvez envoyer votre déclaration maintenant et déposer les justificatifs manquants plus tard, depuis « Mes sinistres ».';

/**
 * US-24, étape 3 « Justificatifs et récapitulatif » (maquettes « Déclarer – 3 » et « Déclaration sur téléphone »).
 * AC7, AC14 à AC16 : pièces attendues du type (obligatoires et facultatives), dépôt de PDF, JPG ou PNG de 10 Mo au plus
 * ou photo prise sur mobile ; une pièce manquante n'empêche pas l'envoi. AC9 : récapitulatif complet, déclaration sur
 * l'honneur jamais pré-cochée (RG8), puis « Envoyer ma déclaration ». Un dossier déjà envoyé n'affiche que ses
 * justificatifs, pour compléter ceux qui manquent.
 */
@Component({
  selector: 'iard-sinistre-justificatifs',
  imports: [RouterLink, BandeauUrgence, EtapesSinistre],
  styleUrls: ['../mes-devis/mes-devis.scss', '../sinistre-commun/sinistre.scss'],
  templateUrl: './sinistre-justificatifs.html',
})
export class SinistreJustificatifs {
  private readonly sinistres = inject(SinistreService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly declaration = signal<DeclarationSinistre | null>(null);
  protected readonly erreur = signal<string | null>(null);
  /** AC15, AC16 : message sous la zone de dépôt de chaque pièce. */
  protected readonly messagesPieces = signal<Record<string, string>>({});
  protected readonly depotEnCours = signal<string | null>(null);
  protected readonly honneur = signal(false);
  protected readonly enCours = signal(false);
  protected readonly messageEnvoi = signal<string | null>(null);

  protected readonly envoyee = computed(() => this.declaration()?.statut === 'DECLARE');
  protected readonly details = computed(() => Object.entries(this.declaration()?.details ?? {}));

  protected readonly ACCEPT_FICHIERS = ACCEPT_FICHIERS;
  protected readonly ADRESSE_SINISTRES = ADRESSE_SINISTRES;
  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly LIBELLES_DECLARANT = LIBELLES_DECLARANT;
  protected readonly MESSAGE_BLESSES = MESSAGE_BLESSES;
  protected readonly MESSAGE_PIECES_PLUS_TARD = MESSAGE_PIECES_PLUS_TARD;
  protected readonly TEXTE_HONNEUR = TEXTE_HONNEUR;
  protected readonly MENTION_HONNEUR = MENTION_HONNEUR;
  protected readonly jourFrancais = jourFrancais;
  protected readonly heureFrancaise = heureFrancaise;
  protected readonly libelleFormule = libelleFormule;
  protected readonly taille = taille;

  constructor() {
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

  protected fichiersDe(piece: PieceAttendue): PieceJointe[] {
    return this.declaration()?.piecesJointes.filter((jointe) => jointe.typePiece === piece.code) ?? [];
  }

  protected lien(etape: 'type' | 'circonstances'): string[] {
    return [ADRESSE_SINISTRES, String(this.declaration()?.id), etape];
  }

  /** AC14 à AC16 : dépôt d'un fichier (contrôlé ici d'abord, puis par le serveur sur son contenu réel). */
  protected deposer(piece: PieceAttendue, evenement: Event): void {
    const champ = evenement.target as HTMLInputElement;
    const fichier = champ.files?.[0];
    champ.value = '';
    const declaration = this.declaration();
    if (!fichier || !declaration || this.depotEnCours()) return;
    const refus = refusFichier(fichier, declaration.piecesJointes.length);
    if (refus) {
      this.messagePiece(piece.code, refus);
      return;
    }
    this.messagePiece(piece.code, null);
    this.depotEnCours.set(piece.code);
    this.sinistres.deposer(declaration.id, piece.code, fichier).subscribe((resultat) => {
      this.depotEnCours.set(null);
      if (resultat.statut === 'ok') {
        this.declaration.set(resultat.valeur);
      } else {
        this.messagePiece(piece.code, resultat.message);
      }
    });
  }

  /** AC14 : « Supprimer » (brouillon seulement). */
  protected supprimer(jointe: PieceJointe): void {
    const declaration = this.declaration();
    if (!declaration || this.envoyee()) return;
    this.sinistres.supprimer(declaration.id, jointe.id).subscribe((resultat) => {
      if (resultat.statut === 'ok') {
        this.declaration.set(resultat.valeur);
      } else {
        this.messagePiece(jointe.typePiece, resultat.message);
      }
    });
  }

  /** Ouvre le fichier déposé dans un nouvel onglet. */
  protected ouvrir(jointe: PieceJointe): void {
    const declaration = this.declaration();
    if (!declaration) return;
    this.sinistres.telecharger(declaration.id, jointe.id).subscribe((resultat) => {
      if (resultat.statut === 'ok') {
        window.open(URL.createObjectURL(resultat.valeur), '_blank', 'noopener');
      } else {
        this.messagePiece(jointe.typePiece, resultat.message);
      }
    });
  }

  protected cocher(evenement: Event): void {
    this.honneur.set((evenement.target as HTMLInputElement).checked);
    this.messageEnvoi.set(null);
  }

  /** AC9 : envoi (déclaration sur l'honneur cochée), puis « Déclaration envoyée ». */
  protected envoyer(): void {
    const declaration = this.declaration();
    if (!declaration || this.enCours()) return;
    if (!this.honneur()) {
      this.messageEnvoi.set(MESSAGE_HONNEUR);
      return;
    }
    this.enCours.set(true);
    this.sinistres.envoyer(declaration.id, true).subscribe((resultat) => {
      this.enCours.set(false);
      if (resultat.statut === 'ok') {
        void this.router.navigate([ADRESSE_SINISTRES, String(declaration.id), 'envoyee'], {
          state: { envoi: resultat.valeur },
        });
      } else {
        this.messageEnvoi.set(resultat.message);
      }
    });
  }

  /** AC8 : « Enregistrer et terminer plus tard ». */
  protected plusTard(): void {
    const declaration = this.declaration();
    if (!declaration || this.enCours()) return;
    this.enCours.set(true);
    this.sinistres.enregistrer(declaration.id, { ...reponsesDe(declaration), etape: 3 }).subscribe((resultat) => {
      this.enCours.set(false);
      if (resultat.statut === 'ok') {
        void this.router.navigate([ADRESSE_SINISTRES], { state: { message: MESSAGE_ENREGISTRE } });
      } else {
        this.messageEnvoi.set(resultat.message);
      }
    });
  }

  private messagePiece(code: string, message: string | null): void {
    const messages = { ...this.messagesPieces() };
    if (message) {
      messages[code] = message;
    } else {
      delete messages[code];
    }
    this.messagesPieces.set(messages);
  }
}
