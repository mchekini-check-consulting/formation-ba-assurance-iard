import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { Avenant, dateJjMmAaaa } from '../../core/model/avenant';
import { ContratResume } from '../../core/model/contrat';
import { DocumentClient, libelleCategorie } from '../../core/model/document';
import { ConnexionService } from '../../core/service/connexion.service';
import { ContratService } from '../../core/service/contrat.service';
import {
  DocumentsContratService,
  MESSAGE_EN_PREPARATION,
  MESSAGE_ERREUR_TELECHARGEMENT,
  enregistrerFichier,
} from '../../core/service/documents-contrat.service';
import { DocumentsService } from '../../core/service/documents.service';

/** US-23 : un avenant accepté et son contrat. */
interface AvenantDuContrat {
  numeroContrat: string;
  avenant: Avenant;
}

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
  private readonly documentsContrat = inject(DocumentsContratService);
  /** US-23 : avenants acceptés des contrats du client, avec leur PDF. */
  protected readonly avenants = signal<AvenantDuContrat[]>([]);
  protected readonly messagesAvenants = signal<Record<number, string>>({});
  protected readonly dateJjMmAaaa = dateJjMmAaaa;

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
      this.avenants.set(
        (contrats ?? []).flatMap((contrat) =>
          (contrat.avenants ?? []).map((avenant) => ({ numeroContrat: contrat.numero, avenant })),
        ),
      );
      this.chargement.set(false);
      this.liste.set(documents);
    });
  }

  /** US-23 : télécharge le PDF d'un avenant accepté (route de T-03, contrôle du propriétaire). */
  protected telechargerAvenant(ligne: AvenantDuContrat): void {
    const id = ligne.avenant.documentId;
    if (id === null) return;
    this.messagesAvenants.update((messages) => ({ ...messages, [id]: '' }));
    this.documentsContrat.telecharger(id).subscribe((resultat) => {
      if (resultat.statut === 'telecharge') {
        enregistrerFichier(resultat.contenu, `avenant-${ligne.avenant.numero}.pdf`);
        return;
      }
      const texte = resultat.statut === 'en-preparation' ? MESSAGE_EN_PREPARATION : MESSAGE_ERREUR_TELECHARGEMENT;
      this.messagesAvenants.update((messages) => ({ ...messages, [id]: texte }));
    });
  }
}
