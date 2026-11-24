import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DocumentClient, libelleCategorie } from '../../core/model/document';
import { DocumentsService } from '../../core/service/documents.service';
import { CadreEspace } from '../cadre-espace/cadre-espace';

/** US-04, AC1 : les documents déposés par le client connecté, et seulement les siens. */
@Component({
  selector: 'iard-mes-documents',
  imports: [RouterLink, CadreEspace],
  styleUrls: ['../mes-devis/mes-devis.scss'],
  templateUrl: './mes-documents.html',
})
export class MesDocuments {
  protected readonly liste = signal<DocumentClient[] | null>(null);
  protected readonly chargement = signal(true);
  protected readonly libelleCategorie = libelleCategorie;

  constructor() {
    inject(DocumentsService)
      .lister()
      .subscribe((liste) => {
        this.chargement.set(false);
        this.liste.set(liste);
      });
  }
}
