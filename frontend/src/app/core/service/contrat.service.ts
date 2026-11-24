import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import { Contrat, ContratResume, DemandeContrat, FicheContrat, Regeneration } from '../model/contrat';

/** US-21 : régénération des conditions particulières. */
export type ResultatRegeneration =
  | { statut: 'regenere'; regeneration: Regeneration }
  /** 429 : une régénération date de moins d'une minute. */
  | { statut: 'trop-tot'; message: string }
  /** 422 : le statut du contrat ne permet pas la régénération (RG2, RG3 d'US-18). */
  | { statut: 'impossible'; message: string }
  | { statut: 'erreur-technique' };

export const MESSAGE_TROP_TOT = "Vos conditions particulières viennent d'être régénérées. Réessayez dans une minute.";
export const MESSAGE_IMPOSSIBLE =
  "La régénération des conditions particulières n'est pas possible pour ce contrat dans son statut actuel.";

export type ResultatFiche =
  | { statut: 'obtenu'; fiche: FicheContrat }
  | { statut: 'introuvable' }
  | { statut: 'erreur-technique' };

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

  /** US-04, AC1 / US-18 : les contrats habitation et auto du client connecté ; null s'ils n'ont pas pu être chargés. */
  lister(): Observable<ContratResume[] | null> {
    return this.http.get<ContratResume[]>(`${apiBase('assurance-iard-api')}/contrats`).pipe(catchError(() => of(null)));
  }

  /**
   * US-21, AC1 : régénère les conditions particulières (nouvelle version, envoyée par e-mail). Un refus (403) est
   * affiché par la page « Accès non autorisé ».
   */
  regenerer(numero: string): Observable<ResultatRegeneration> {
    return this.http
      .post<Regeneration>(
        `${apiBase('assurance-iard-api')}/contrats/${encodeURIComponent(numero)}/conditions-particulieres`,
        null,
      )
      .pipe(
        map((regeneration): ResultatRegeneration => ({ statut: 'regenere', regeneration })),
        catchError((erreur: HttpErrorResponse) => {
          const detail = (erreur.error as { detail?: string } | null)?.detail;
          if (erreur.status === 429) {
            return of<ResultatRegeneration>({ statut: 'trop-tot', message: detail ?? MESSAGE_TROP_TOT });
          }
          if (erreur.status === 422) {
            return of<ResultatRegeneration>({ statut: 'impossible', message: detail ?? MESSAGE_IMPOSSIBLE });
          }
          return of<ResultatRegeneration>({ statut: 'erreur-technique' });
        }),
      );
  }

  /** US-18, AC3 : fiche d'un contrat par son numéro. Un refus (403) est affiché par la page « Accès non autorisé ». */
  obtenirFiche(numero: string): Observable<ResultatFiche> {
    return this.http.get<FicheContrat>(`${apiBase('assurance-iard-api')}/contrats/${encodeURIComponent(numero)}`).pipe(
      map((fiche): ResultatFiche => ({ statut: 'obtenu', fiche })),
      catchError((erreur: HttpErrorResponse) =>
        of<ResultatFiche>(erreur.status === 404 ? { statut: 'introuvable' } : { statut: 'erreur-technique' }),
      ),
    );
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
