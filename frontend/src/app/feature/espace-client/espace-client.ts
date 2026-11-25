import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { adresseSiLivree, tuiles } from '../../core/config/raccourcis';
import { ContratResume, LIBELLES_PRODUIT, StatutContrat, libelleFormule } from '../../core/model/contrat';
import { formaterDate } from '../../core/model/devis-enregistre';
import { formaterNomPropre } from '../../core/model/identite';
import { formaterPrix } from '../../core/model/recommandation';
import {
  FactureResume,
  LIBELLE_SINISTRE_DECLARE,
  TableauDeBord,
  libelleSinistre,
} from '../../core/model/tableau-de-bord';
import { ConnexionService } from '../../core/service/connexion.service';
import {
  DocumentsContratService,
  MESSAGE_EN_PREPARATION,
  MESSAGE_ERREUR_TELECHARGEMENT,
  enregistrerFichier,
} from '../../core/service/documents-contrat.service';
import { EspaceClientService } from '../../core/service/espace-client.service';
import { CarteContrat } from '../carte-contrat/carte-contrat';
import { TextePlaque } from '../texte-plaque/texte-plaque';

type Etat = 'chargement' | 'affiche' | 'erreur' | 'sans-droit';

/** Nombre de contrats sur le tableau de bord (recette US-18). */
export const CONTRATS_AFFICHES = 4;
/** Statuts qui demandent une action du client : affichés en premier. */
export const STATUTS_A_TRAITER: readonly StatutContrat[] = [
  'EN_ATTENTE_DE_SIGNATURE',
  'AVENANT_EN_ATTENTE',
  'IMPAYE',
  'SUSPENDU',
];

/**
 * US-18 : tableau de bord du client connecté (AC1 prénom, contrats, devis en cours, factures, sinistres ; AC2
 * raccourcis ; AC5 statuts ; AC6 sans contrat ; AC7 ses données seulement, filtrées par le serveur). Mise en page de
 * la maquette validée : contrats en grille, puis échéance, devis et sinistres, puis « Que voulez-vous faire ? ».
 */
@Component({
  selector: 'iard-espace-client',
  imports: [RouterLink, CarteContrat, TextePlaque],
  styleUrl: './espace-client.scss',
  templateUrl: './espace-client.html',
})
export class EspaceClient {
  private readonly connexionService = inject(ConnexionService);
  private readonly documents = inject(DocumentsContratService);

  protected readonly etat = signal<Etat>('chargement');
  protected readonly tableau = signal<TableauDeBord | null>(null);
  /** Message de téléchargement par numéro de facture. */
  protected readonly messagesFactures = signal<Record<string, string>>({});

  protected readonly prenom = computed(() => formaterNomPropre(this.tableau()?.prenom));

  /**
   * Au plus 4 contrats : d'abord ceux qui demandent une action du client, puis les autres, du plus récent au plus
   * ancien (ordre du serveur, conservé dans chaque groupe). « Tout voir » mène à Mes contrats, qui affiche tout.
   */
  protected readonly contratsAffiches = computed(() => {
    const contrats = this.tableau()?.contrats ?? [];
    const urgent = (c: ContratResume): number => (STATUTS_A_TRAITER.includes(c.statut) ? 0 : 1);
    return [...contrats].sort((a, b) => urgent(a) - urgent(b)).slice(0, CONTRATS_AFFICHES);
  });

  /** Le contrat dont l'échéance annuelle arrive le plus tôt. */
  protected readonly prochaineEcheance = computed(() => {
    const contrats = (this.tableau()?.contrats ?? []).filter((c) => c.dateEcheance);
    return contrats.sort((a, b) => (a.dateEcheance ?? '').localeCompare(b.dateEcheance ?? ''))[0] ?? null;
  });
  /** Le devis en cours le plus récent (le serveur les renvoie du plus récent au plus ancien). */
  protected readonly devisRecent = computed(() => this.tableau()?.devisEnCours[0] ?? null);
  protected readonly produitDuContrat = computed(() => {
    const produits = new Map((this.tableau()?.contrats ?? []).map((c) => [c.numero, c.produit]));
    return (numero: string) => produits.get(numero) ?? null;
  });

  protected readonly tuiles = tuiles();
  protected readonly adresseFactures = adresseSiLivree('factures');
  protected readonly adresseSinistres = adresseSiLivree('sinistres');

  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly LIBELLE_SINISTRE_DECLARE = LIBELLE_SINISTRE_DECLARE;
  protected readonly libelleFormule = libelleFormule;
  protected readonly libelleSinistre = libelleSinistre;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;

  constructor() {
    if (!this.connexionService.aLeDroit('CONTRATS', 'CONSULTER')) {
      this.etat.set('sans-droit');
      return;
    }
    inject(EspaceClientService)
      .tableauDeBord(this.connexionService.emailClient())
      .subscribe((tableau) => {
        this.tableau.set(tableau);
        this.etat.set(tableau ? 'affiche' : 'erreur');
      });
  }

  protected telechargerFacture(facture: FactureResume): void {
    if (facture.documentId === null) return;
    this.messagesFactures.update((messages) => ({ ...messages, [facture.numero]: '' }));
    this.documents.telecharger(facture.documentId).subscribe((resultat) => {
      if (resultat.statut === 'telecharge') {
        enregistrerFichier(resultat.contenu, `${facture.numero}.pdf`);
        return;
      }
      const message = resultat.statut === 'en-preparation' ? MESSAGE_EN_PREPARATION : MESSAGE_ERREUR_TELECHARGEMENT;
      this.messagesFactures.update((messages) => ({ ...messages, [facture.numero]: message }));
    });
  }
}
