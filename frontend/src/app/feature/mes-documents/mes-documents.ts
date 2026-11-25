import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { ContratResume } from '../../core/model/contrat';
import { DocumentClient, libelleCategorie } from '../../core/model/document';
import { ConnexionService } from '../../core/service/connexion.service';
import { ContratService } from '../../core/service/contrat.service';
import { DocumentsService } from '../../core/service/documents.service';

/** Documents d'une même souscription (même référence de devis). */
interface GroupeDocuments {
  referenceDevis: string;
  /** « Contrat AE-… » si la souscription a donné un contrat, sinon « Souscription en cours ». */
  titre: string;
  documents: DocumentClient[];
}

export const TITRE_SOUSCRIPTION_EN_COURS = 'Souscription en cours';

/**
 * US-04, AC1 : les documents déposés par le client connecté, et seulement les siens.
 * US-18 (recette) : liste compacte, une ligne par document, regroupée par souscription. Le titre d'un groupe est le
 * numéro du contrat issu de la souscription (liste « Mes contrats », route existante) ; aucune référence technique
 * n'est affichée au client.
 */
@Component({
  selector: 'iard-mes-documents',
  imports: [RouterLink],
  styleUrls: ['../mes-devis/mes-devis.scss', './mes-documents.scss'],
  templateUrl: './mes-documents.html',
})
export class MesDocuments {
  protected readonly liste = signal<DocumentClient[] | null>(null);
  protected readonly chargement = signal(true);
  protected readonly libelleCategorie = libelleCategorie;
  /** Numéro de contrat par référence de devis de souscription. */
  private readonly contratsParDevis = signal(new Map<string, string>());

  /** Groupes dans l'ordre du serveur (premier document de chaque souscription). */
  protected readonly groupes = computed<GroupeDocuments[]>(() => {
    const contrats = this.contratsParDevis();
    const groupes = new Map<string, GroupeDocuments>();
    for (const document of this.liste() ?? []) {
      const reference = document.referenceDevis ?? '';
      if (!groupes.has(reference)) {
        const numero = contrats.get(reference);
        groupes.set(reference, {
          referenceDevis: reference,
          titre: numero ? `Contrat ${numero}` : TITRE_SOUSCRIPTION_EN_COURS,
          documents: [],
        });
      }
      groupes.get(reference)!.documents.push(document);
    }
    return [...groupes.values()];
  });

  constructor() {
    // Les contrats ne sont lus qu'avec le droit de les consulter (sinon le serveur refuserait l'accès, US-04).
    const peutLireContrats = inject(ConnexionService).aLeDroit('CONTRATS', 'CONSULTER');
    forkJoin({
      documents: inject(DocumentsService).lister(),
      contrats: peutLireContrats ? inject(ContratService).lister() : of<ContratResume[] | null>([]),
    }).subscribe(({ documents, contrats }) => {
      this.contratsParDevis.set(
        new Map((contrats ?? []).filter((c) => c.referenceDevis).map((c) => [c.referenceDevis!, c.numero])),
      );
      this.chargement.set(false);
      this.liste.set(documents);
    });
  }
}
