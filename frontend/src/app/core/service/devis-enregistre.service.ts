import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import { DemandeDevis, DevisEnregistre, RepriseDevis } from '../model/devis-enregistre';

export type ResultatEnregistrement =
  | { statut: 'enregistre'; dateFinValidite: string }
  | { statut: 'session-expiree' }
  | { statut: 'erreur' };

@Injectable({ providedIn: 'root' })
export class DevisEnregistreService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('assurance-iard-api')}/devis`;

  enregistrer(demande: DemandeDevis): Observable<ResultatEnregistrement> {
    return this.http.post<DevisEnregistre>(`${this.url}/enregistres`, demande).pipe(
      map(({ dateFinValidite }): ResultatEnregistrement => ({ statut: 'enregistre', dateFinValidite })),
      catchError((erreur: unknown) =>
        of<ResultatEnregistrement>({
          statut: erreur instanceof HttpErrorResponse && erreur.status === 401 ? 'session-expiree' : 'erreur',
        }),
      ),
    );
  }

  /** Renvoie null en cas d'échec. */
  lister(): Observable<DevisEnregistre[] | null> {
    return this.http.get<DevisEnregistre[]>(`${this.url}/enregistres`).pipe(catchError(() => of(null)));
  }

  /** Renvoie null en cas d'échec. */
  reprendre(reference: string): Observable<RepriseDevis | null> {
    return this.http
      .post<RepriseDevis>(`${this.url}/enregistres/${reference}/reprise`, null)
      .pipe(catchError(() => of(null)));
  }

  supprimer(reference: string): Observable<boolean> {
    return this.http.delete<void>(`${this.url}/enregistres/${reference}`).pipe(
      map(() => true),
      catchError(() => of(false)),
    );
  }

  /** Renvoie null en cas d'échec. */
  telechargerPdf(demande: DemandeDevis): Observable<Blob | null> {
    return this.http.post(`${this.url}/pdf`, demande, { responseType: 'blob' }).pipe(catchError(() => of(null)));
  }
}
