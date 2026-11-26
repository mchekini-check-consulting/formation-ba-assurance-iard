import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ADRESSES, CHEMIN_AVENANT_ACCEPTE, CHEMIN_GARANTIES } from '../../core/config/raccourcis';
import { GarantiesContrat, dateJjMmAaaa, messageApplication, montantTtc } from '../../core/model/avenant';
import { DocumentContrat, LIBELLES_PRODUIT } from '../../core/model/contrat';
import { formaterNomPropre } from '../../core/model/identite';
import { AvenantService } from '../../core/service/avenant.service';
import {
  DocumentsContratService,
  MESSAGE_EN_PREPARATION,
  MESSAGE_ERREUR_TELECHARGEMENT,
  enregistrerFichier,
} from '../../core/service/documents-contrat.service';

/**
 * US-23, AC3, AC4 et AC5 : « Relire et accepter l'avenant ». Ancienne et nouvelle cotisation, date d'effet et
 * application (RG3), garanties ajoutées ou retirées, conseil mis à jour si la couverture diminue (RG4), documents à
 * relire (fiche conseil, IPID, conditions générales). RG1 : la case « J'ai lu et j'accepte mon avenant » n'est jamais
 * pré-cochée et « Accepter l'avenant » reste désactivé tant qu'elle ne l'est pas. RG2 : pas de signature par code.
 * L'avenant peut aussi être abandonné ; l'écran se met à jour sans recharger la page.
 */
@Component({
  selector: 'iard-relire-avenant',
  imports: [RouterLink],
  styleUrls: [
    '../mes-devis/mes-devis.scss',
    '../contrat-client/contrat-client.scss',
    '../modifier-garanties/modifier-garanties.scss',
  ],
  templateUrl: './relire-avenant.html',
})
export class RelireAvenant {
  private readonly avenants = inject(AvenantService);
  private readonly documents = inject(DocumentsContratService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly numero = signal('');
  protected readonly contrat = signal<GarantiesContrat | null>(null);
  protected readonly erreur = signal<string | null>(null);
  /** RG1 : jamais pré-cochée. */
  protected readonly accepte = signal(false);
  protected readonly envoiEnCours = signal(false);
  protected readonly message = signal<{ succes: boolean; texte: string } | null>(null);
  protected readonly messagesDocuments = signal<Record<number, string>>({});

  protected readonly projet = computed(() => this.contrat()?.avenantEnAttente ?? null);
  protected readonly prenom = computed(() => formaterNomPropre(this.contrat()?.prenom));
  protected readonly lienFiche = computed(() => [ADRESSES.modifierGaranties ?? '/espace-client/contrats', this.numero()]);
  protected readonly lienGaranties = computed(() => [...this.lienFiche(), CHEMIN_GARANTIES]);
  protected readonly application = computed(() => {
    const projet = this.projet();
    return projet ? messageApplication(projet) : '';
  });
  protected readonly adresseIpid = computed(() => {
    const formule = this.projet()?.ipidFormule;
    return formule ? this.avenants.adresseIpid(formule) : null;
  });

  protected readonly montantTtc = montantTtc;
  protected readonly dateJjMmAaaa = dateJjMmAaaa;
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
    this.accepte.set(false);
    this.avenants.garanties(this.numero()).subscribe((resultat) => {
      if (resultat.statut === 'obtenu') {
        this.contrat.set(resultat.garanties);
      } else {
        this.erreur.set(resultat.message);
      }
    });
  }

  protected cocher(evenement: Event): void {
    this.accepte.set((evenement.target as HTMLInputElement).checked);
  }

  /** AC4 : accepte l'avenant, puis ouvre « Avenant accepté » (sans recharger la page). */
  protected accepter(): void {
    if (!this.accepte() || this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.message.set(null);
    this.avenants.accepter(this.numero()).subscribe((resultat) => {
      this.envoiEnCours.set(false);
      if (resultat.statut === 'accepte') {
        void this.router.navigate([...this.lienFiche(), CHEMIN_AVENANT_ACCEPTE], {
          state: { acceptation: resultat.acceptation },
        });
      } else {
        this.message.set({ succes: false, texte: resultat.message });
      }
    });
  }

  /** AC5 : abandonne l'avenant ; le contrat reste aux conditions actuelles. */
  protected abandonner(): void {
    if (this.envoiEnCours()) return;
    this.envoiEnCours.set(true);
    this.message.set(null);
    this.avenants.abandonner(this.numero()).subscribe((resultat) => {
      this.envoiEnCours.set(false);
      if (resultat.statut === 'abandonne') {
        this.contrat.update((contrat) => (contrat ? { ...contrat, avenantEnAttente: null } : contrat));
        this.message.set({
          succes: true,
          texte: `Votre avenant ${resultat.avenant.numero} est abandonné : votre contrat reste aux conditions actuelles.`,
        });
      } else {
        this.message.set({ succes: false, texte: resultat.message });
      }
    });
  }

  protected telecharger(document: DocumentContrat): void {
    this.messagesDocuments.update((messages) => ({ ...messages, [document.id]: '' }));
    this.documents.telecharger(document.id).subscribe((resultat) => {
      if (resultat.statut === 'telecharge') {
        enregistrerFichier(resultat.contenu, `${document.type.toLowerCase().replace(/_/g, '-')}-${this.numero()}.pdf`);
        return;
      }
      const texte = resultat.statut === 'en-preparation' ? MESSAGE_EN_PREPARATION : MESSAGE_ERREUR_TELECHARGEMENT;
      this.messagesDocuments.update((messages) => ({ ...messages, [document.id]: texte }));
    });
  }
}
