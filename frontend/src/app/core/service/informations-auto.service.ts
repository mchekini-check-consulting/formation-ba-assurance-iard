import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';

export interface InformationsAutoReponse {
  civilite: string | null;
  nom: string | null;
  prenom: string | null;
  dateNaissance: string | null;
  telephone: string | null;
  adresse: string | null;
  codePostal: string | null;
  ville: string | null;
}

export interface DemandeInformationsAuto {
  civilite: string;
  nom: string;
  prenom: string;
  dateNaissance: string;
  telephone: string;
  adresse: string;
  codePostal: string;
  ville: string;
}

export type ResultatInformationsAuto =
  | { statut: 'enregistre' }
  | { statut: 'refuse'; champ: string; message: string }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

@Injectable({ providedIn: 'root' })
export class InformationsAutoService {
  private readonly http = inject(HttpClient);

  /** Pré-remplissage : un échec de lecture laisse simplement les champs vides. */
  lire(devisAutoId: number): Observable<InformationsAutoReponse | null> {
    return this.http
      .get<InformationsAutoReponse>(`${apiBase('assurance-iard-api')}/v1/devis/auto/${devisAutoId}/souscripteur`)
      .pipe(catchError(() => of(null)));
  }

  enregistrer(devisAutoId: number, demande: DemandeInformationsAuto): Observable<ResultatInformationsAuto> {
    return this.http
      .post<void>(`${apiBase('assurance-iard-api')}/v1/devis/auto/${devisAutoId}/souscripteur`, demande)
      .pipe(
        map((): ResultatInformationsAuto => ({ statut: 'enregistre' })),
        catchError((erreur: HttpErrorResponse) => of(this.resultatRefus(erreur))),
      );
  }

  private resultatRefus(erreur: HttpErrorResponse): ResultatInformationsAuto {
    if (erreur.status === 401) return { statut: 'session-expiree' };
    if (erreur.status === 422) {
      return { statut: 'refuse', champ: erreur.error.champ, message: erreur.error.detail };
    }
    return { statut: 'erreur-technique' };
  }
}
