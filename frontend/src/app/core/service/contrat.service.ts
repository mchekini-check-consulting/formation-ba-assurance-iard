import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import { Contrat, DemandeContrat } from '../model/contrat';

export type ResultatGeneration =
  | { statut: 'genere'; contrat: Contrat }
  | { statut: 'mandat-non-signe' }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

export type ResultatObtention =
  | { statut: 'obtenu'; contrat: Contrat }
  | { statut: 'introuvable' }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

export type ResultatSignature =
  | { statut: 'signe'; contrat: Contrat }
  | { statut: 'code-incorrect' }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

@Injectable({ providedIn: 'root' })
export class ContratService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('backend')}/devis/contrat`;

  generer(demande: DemandeContrat): Observable<ResultatGeneration> {
    return this.http.post<Contrat>(this.url, demande).pipe(
      map((contrat): ResultatGeneration => ({ statut: 'genere', contrat })),
      catchError((erreur: HttpErrorResponse) => of(this.resultatErreurGeneration(erreur))),
    );
  }

  obtenir(referenceDevis: string): Observable<ResultatObtention> {
    return this.http.get<Contrat>(`${this.url}/${referenceDevis}`).pipe(
      map((contrat): ResultatObtention => ({ statut: 'obtenu', contrat })),
      catchError((erreur: HttpErrorResponse) =>
        of<ResultatObtention>(
          erreur.status === 401
            ? { statut: 'session-expiree' }
            : erreur.status === 404
              ? { statut: 'introuvable' }
              : { statut: 'erreur-technique' },
        ),
      ),
    );
  }

  signer(referenceDevis: string, code: string): Observable<ResultatSignature> {
    return this.http.post<Contrat>(`${this.url}/${referenceDevis}/signature`, { code }).pipe(
      map((contrat): ResultatSignature => ({ statut: 'signe', contrat })),
      catchError((erreur: HttpErrorResponse) =>
        of<ResultatSignature>(
          erreur.status === 401
            ? { statut: 'session-expiree' }
            : erreur.status === 422
              ? { statut: 'code-incorrect' }
              : { statut: 'erreur-technique' },
        ),
      ),
    );
  }

  private resultatErreurGeneration(erreur: HttpErrorResponse): ResultatGeneration {
    if (erreur.status === 401) return { statut: 'session-expiree' };
    if (erreur.status === 422) return { statut: 'mandat-non-signe' };
    return { statut: 'erreur-technique' };
  }
}
