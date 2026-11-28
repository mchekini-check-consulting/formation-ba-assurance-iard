import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { apiBase } from '../config/api';
import { ContratAutoDetail } from '../model/devis-auto';

export type ErreurSignature =
  | 'session-expiree'
  | 'code-incorrect'
  | 'essais-epuises'
  | 'code-expire'
  | 'erreur-serveur';

export interface ResultatEnvoiCode {
  emailMasque: string;
}

export interface ErreurAvecEssais {
  code: ErreurSignature;
  essaisRestants?: number;
}

@Injectable({ providedIn: 'root' })
export class ContratAutoService {
  private readonly http = inject(HttpClient);

  private url(devisAutoId: number): string {
    return `${apiBase('assurance-iard-api')}/v1/devis/auto/${devisAutoId}/contrat`;
  }

  obtenir(devisAutoId: number): Observable<ContratAutoDetail> {
    return this.http.get<ContratAutoDetail>(`${this.url(devisAutoId)}/signature`);
  }

  envoyerCode(devisAutoId: number): Observable<ResultatEnvoiCode> {
    return this.http
      .post<ResultatEnvoiCode>(`${this.url(devisAutoId)}/signature/code`, {})
      .pipe(catchError(this.traiterErreur));
  }

  renvoyerCode(devisAutoId: number): Observable<ResultatEnvoiCode> {
    return this.http
      .post<ResultatEnvoiCode>(`${this.url(devisAutoId)}/signature/code/renvoyer`, {})
      .pipe(catchError(this.traiterErreur));
  }

  signer(devisAutoId: number, code: string): Observable<ContratAutoDetail> {
    return this.http
      .post<ContratAutoDetail>(`${this.url(devisAutoId)}/signature`, { code })
      .pipe(catchError(this.traiterErreur));
  }

  private traiterErreur(err: HttpErrorResponse): Observable<never> {
    if (err.status === 401) return throwError(() => ({ code: 'session-expiree' }));
    if (err.status === 410) return throwError(() => ({ code: 'code-expire' }));
    if (err.status === 409) return throwError(() => ({ code: 'essais-epuises' }));
    if (err.status === 422) {
      const essaisRestants: number | undefined = err.error?.essaisRestants;
      return throwError(() => ({ code: 'code-incorrect', essaisRestants }));
    }
    return throwError(() => ({ code: 'erreur-serveur' }));
  }
}
