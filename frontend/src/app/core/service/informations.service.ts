import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import { IdentiteCompte } from '../model/informations';

export interface DemandeInformations {
  referenceDevis: string;
  civilite: string;
  nom: string;
  prenom: string;
  dateNaissance: string;
  telephone: string;
  typePieceIdentite: string;
  typeJustificatifDomicile: string;
  colocataires: string[];
}

export type ResultatInformations =
  | { statut: 'enregistre' }
  | { statut: 'refuse'; champ: string; message: string }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

@Injectable({ providedIn: 'root' })
export class InformationsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('backend')}/devis/informations`;

  /** AC1 : un échec de lecture laisse simplement les champs vides. */
  lire(): Observable<IdentiteCompte | null> {
    return this.http.get<IdentiteCompte>(this.url).pipe(catchError(() => of(null)));
  }

  enregistrer(
    demande: DemandeInformations,
    pieceIdentite: File,
    justificatifDomicile: File,
  ): Observable<ResultatInformations> {
    const envoi = new FormData();
    envoi.append('informations', new Blob([JSON.stringify(demande)], { type: 'application/json' }));
    envoi.append('pieceIdentite', pieceIdentite);
    envoi.append('justificatifDomicile', justificatifDomicile);
    return this.http.post<void>(this.url, envoi).pipe(
      map((): ResultatInformations => ({ statut: 'enregistre' })),
      catchError((erreur: HttpErrorResponse) => of(this.resultatRefus(erreur))),
    );
  }

  private resultatRefus(erreur: HttpErrorResponse): ResultatInformations {
    if (erreur.status === 401) return { statut: 'session-expiree' };
    if (erreur.status === 422) {
      return { statut: 'refuse', champ: erreur.error.champ, message: erreur.error.detail };
    }
    return { statut: 'erreur-technique' };
  }
}
