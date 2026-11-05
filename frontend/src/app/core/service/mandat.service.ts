import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';

export interface MandatPrepare {
  creancier: string;
  ics: string;
  rum: string;
}

export interface DemandeSignature {
  referenceDevis: string;
  titulaire: string;
  iban: string;
  dateDebut: string;
  jourPrelevement: number;
  autorisation: boolean;
}

export type ResultatPreparation =
  | { statut: 'prepare'; mandat: MandatPrepare }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

export type ResultatSignature =
  | { statut: 'signe' }
  | { statut: 'refuse'; champ: string; message: string }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

@Injectable({ providedIn: 'root' })
export class MandatService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('backend')}/devis/mandats`;

  preparer(referenceDevis: string): Observable<ResultatPreparation> {
    return this.http.post<MandatPrepare>(this.url, { referenceDevis }).pipe(
      map((mandat): ResultatPreparation => ({ statut: 'prepare', mandat })),
      catchError((erreur: HttpErrorResponse) =>
        of<ResultatPreparation>({ statut: erreur.status === 401 ? 'session-expiree' : 'erreur-technique' }),
      ),
    );
  }

  signer(demande: DemandeSignature): Observable<ResultatSignature> {
    return this.http.post<unknown>(`${this.url}/signature`, demande).pipe(
      map((): ResultatSignature => ({ statut: 'signe' })),
      catchError((erreur: HttpErrorResponse) => of(this.resultatRefus(erreur))),
    );
  }

  private resultatRefus(erreur: HttpErrorResponse): ResultatSignature {
    if (erreur.status === 401) return { statut: 'session-expiree' };
    if (erreur.status === 422) {
      return { statut: 'refuse', champ: erreur.error.champ, message: erreur.error.detail };
    }
    return { statut: 'erreur-technique' };
  }
}
