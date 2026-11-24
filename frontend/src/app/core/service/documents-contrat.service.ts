import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';

export type ResultatTelechargement =
  | { statut: 'telecharge'; contenu: Blob }
  /** 409 : le PDF n'est pas encore stocké (juste après le démarrage, ou en cours de génération). */
  | { statut: 'en-preparation' }
  | { statut: 'erreur' };

/**
 * US-18, AC3 : téléchargement d'un PDF généré par T-03 (GET /documents-contrat/{id}). Un refus (403) affiche la page
 * « Accès non autorisé » (intercepteur d'US-04).
 */
@Injectable({ providedIn: 'root' })
export class DocumentsContratService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('assurance-iard-api')}/documents-contrat`;

  telecharger(id: number): Observable<ResultatTelechargement> {
    return this.http.get(`${this.url}/${id}`, { responseType: 'blob' }).pipe(
      map((contenu): ResultatTelechargement => ({ statut: 'telecharge', contenu })),
      catchError((erreur: HttpErrorResponse) =>
        of<ResultatTelechargement>(erreur.status === 409 ? { statut: 'en-preparation' } : { statut: 'erreur' }),
      ),
    );
  }
}

/** Propose le fichier au navigateur sous ce nom, puis libère la mémoire. */
export function enregistrerFichier(contenu: Blob, nom: string): void {
  const adresse = URL.createObjectURL(contenu);
  const lien = document.createElement('a');
  lien.href = adresse;
  lien.download = nom;
  lien.click();
  URL.revokeObjectURL(adresse);
}

export const MESSAGE_EN_PREPARATION = 'Document en préparation, réessayez dans quelques minutes.';
export const MESSAGE_ERREUR_TELECHARGEMENT = "Le document n'a pas pu être téléchargé. Réessayez dans quelques instants.";
