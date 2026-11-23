import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import { Contrat, ContratResume, DemandeContrat } from '../model/contrat';

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

export type ResultatEnvoiCode =
  | { statut: 'envoye'; emailMasque: string }
  | { statut: 'introuvable' }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

export type ResultatSignature =
  | { statut: 'signe'; contrat: Contrat }
  | { statut: 'code-incorrect'; essaisRestants: number | null }
  | { statut: 'code-expire' }
  | { statut: 'essais-epuises' }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

@Injectable({ providedIn: 'root' })
export class ContratService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('assurance-iard-api')}/devis/contrat`;

  /** US-04, AC1 : les contrats du client connecté ; null s'ils n'ont pas pu être chargés. */
  lister(): Observable<ContratResume[] | null> {
    return this.http.get<ContratResume[]>(`${apiBase('assurance-iard-api')}/contrats`).pipe(catchError(() => of(null)));
  }

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

  envoyerCode(referenceDevis: string): Observable<ResultatEnvoiCode> {
    return this.http.post<{ emailMasque: string }>(`${this.url}/${referenceDevis}/signature/code`, null).pipe(
      map((reponse): ResultatEnvoiCode => ({ statut: 'envoye', emailMasque: reponse.emailMasque })),
      catchError((erreur: HttpErrorResponse) =>
        of<ResultatEnvoiCode>(
          erreur.status === 401
            ? { statut: 'session-expiree' }
            : erreur.status === 404
              ? { statut: 'introuvable' }
              : { statut: 'erreur-technique' },
        ),
      ),
    );
  }

  renvoyerCode(referenceDevis: string): Observable<ResultatEnvoiCode> {
    return this.http.post<{ emailMasque: string }>(`${this.url}/${referenceDevis}/signature/code/renvoyer`, null).pipe(
      map((reponse): ResultatEnvoiCode => ({ statut: 'envoye', emailMasque: reponse.emailMasque })),
      catchError((erreur: HttpErrorResponse) =>
        of<ResultatEnvoiCode>(
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
      catchError((erreur: HttpErrorResponse) => of(this.resultatErreurSignature(erreur))),
    );
  }

  private resultatErreurGeneration(erreur: HttpErrorResponse): ResultatGeneration {
    if (erreur.status === 401) return { statut: 'session-expiree' };
    if (erreur.status === 422) return { statut: 'mandat-non-signe' };
    return { statut: 'erreur-technique' };
  }

  private resultatErreurSignature(erreur: HttpErrorResponse): ResultatSignature {
    if (erreur.status === 401) return { statut: 'session-expiree' };
    if (erreur.status === 410) return { statut: 'code-expire' };
    if (erreur.status === 409) return { statut: 'essais-epuises' };
    if (erreur.status === 422) {
      const essaisRestants = (erreur.error as { essaisRestants?: number })?.essaisRestants ?? null;
      return { statut: 'code-incorrect', essaisRestants };
    }
    return { statut: 'erreur-technique' };
  }
}
