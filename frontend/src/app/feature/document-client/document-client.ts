import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DocumentsService } from '../../core/service/documents.service';

/** US-04 : ouvre un document du client. Le document d'un autre client est refusé par le serveur (AC2). */
@Component({
  selector: 'iard-document-client',
  imports: [RouterLink],
  styleUrls: ['../compte-cree/compte-cree.scss', '../mes-devis/mes-devis.scss'],
  templateUrl: './document-client.html',
})
export class DocumentClientPage {
  protected readonly adresse = signal<string | null>(null);
  protected readonly erreur = signal(false);

  constructor() {
    const id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';
    inject(DestroyRef).onDestroy(() => {
      const adresse = this.adresse();
      if (adresse) URL.revokeObjectURL(adresse);
    });
    inject(DocumentsService)
      .ouvrir(id)
      .subscribe((contenu) => {
        if (contenu === null) {
          this.erreur.set(true);
        } else {
          this.adresse.set(URL.createObjectURL(contenu));
        }
      });
  }
}
