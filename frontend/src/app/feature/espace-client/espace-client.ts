import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Droit, Rubrique } from '../../core/model/droits';
import { formaterDate } from '../../core/model/devis-enregistre';
import { LIBELLES_PRODUIT, libelleFormule } from '../../core/model/contrat';
import { formaterPrix } from '../../core/model/recommandation';
import { FactureResume, TableauDeBord, libelleSinistre } from '../../core/model/tableau-de-bord';
import { AnnonceService } from '../../core/service/annonce.service';
import { ConnexionService } from '../../core/service/connexion.service';
import {
  DocumentsContratService,
  MESSAGE_EN_PREPARATION,
  MESSAGE_ERREUR_TELECHARGEMENT,
  enregistrerFichier,
} from '../../core/service/documents-contrat.service';
import { EspaceClientService } from '../../core/service/espace-client.service';
import { CarteContrat } from '../carte-contrat/carte-contrat';

type Etat = 'chargement' | 'affiche' | 'erreur' | 'sans-droit';

/**
 * US-18 : tableau de bord du client connecté (AC1 prénom, contrats, devis en cours, dernières factures, sinistres ;
 * AC2 raccourcis ; AC5 statuts ; AC6 sans contrat ; AC7 ses données seulement, filtrées par le serveur).
 */
@Component({
  selector: 'iard-espace-client',
  imports: [RouterLink, CarteContrat],
  styleUrls: ['../compte-cree/compte-cree.scss', '../mes-devis/mes-devis.scss', './espace-client.scss'],
  templateUrl: './espace-client.html',
})
export class EspaceClient {
  private readonly connexionService = inject(ConnexionService);
  private readonly annonceService = inject(AnnonceService);
  private readonly documents = inject(DocumentsContratService);
  private readonly router = inject(Router);

  protected readonly email = this.connexionService.emailClient;
  protected readonly profil = this.connexionService.profilClient;
  protected readonly etat = signal<Etat>('chargement');
  protected readonly tableau = signal<TableauDeBord | null>(null);
  /** Message de téléchargement par numéro de facture. */
  protected readonly messagesFactures = signal<Record<string, string>>({});

  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly libelleFormule = libelleFormule;
  protected readonly libelleSinistre = libelleSinistre;
  protected readonly formaterDate = formaterDate;
  protected readonly formaterPrix = formaterPrix;

  constructor() {
    if (!this.peut('CONTRATS', 'CONSULTER')) {
      this.etat.set('sans-droit');
      return;
    }
    inject(EspaceClientService)
      .tableauDeBord()
      .subscribe((tableau) => {
        this.tableau.set(tableau);
        this.etat.set(tableau ? 'affiche' : 'erreur');
      });
  }

  protected peut(rubrique: Rubrique, droit: Droit): boolean {
    return this.connexionService.aLeDroit(rubrique, droit);
  }

  protected estInterne(): boolean {
    return this.connexionService.estInterne();
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

  protected seDeconnecter(): void {
    this.connexionService.deconnecter().subscribe(() => {
      this.annonceService.preparer('Vous êtes bien déconnecté.');
      void this.router.navigate(['/']);
    });
  }
}
