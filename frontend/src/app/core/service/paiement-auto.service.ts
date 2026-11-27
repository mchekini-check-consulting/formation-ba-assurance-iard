import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import { ContratAutoGenere, IbanVerification, PaiementAutoInit } from '../model/devis-auto';

export interface DemandePaiementAuto {
  dateDebut: string;
  titulaire: string;
  iban: string;
  bic: string;
  banque: string;
  jourPrelevement: number;
  autorisationPrelevement: boolean;
}

export type ResultatInitPaiement =
  | { statut: 'charge'; data: PaiementAutoInit }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

export type ResultatVerificationIban =
  | { statut: 'verifie'; verification: IbanVerification }
  | { statut: 'erreur-technique' };

export type ResultatGenerationContrat =
  | { statut: 'genere'; contrat: ContratAutoGenere }
  | { statut: 'refuse'; champ: string; message: string }
  | { statut: 'session-expiree' }
  | { statut: 'erreur-technique' };

@Injectable({ providedIn: 'root' })
export class PaiementAutoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('assurance-iard-api')}/v1/devis/auto`;

  charger(devisId: number): Observable<ResultatInitPaiement> {
    return this.http.get<PaiementAutoInit>(`${this.url}/${devisId}/paiement`).pipe(
      map((data): ResultatInitPaiement => ({ statut: 'charge', data })),
      catchError((e: HttpErrorResponse) =>
        of<ResultatInitPaiement>({ statut: e.status === 401 ? 'session-expiree' : 'erreur-technique' }),
      ),
    );
  }

  verifierIban(iban: string): Observable<ResultatVerificationIban> {
    return this.http.post<IbanVerification>(`${this.url}/iban/verifier`, { iban }).pipe(
      map((verification): ResultatVerificationIban => ({ statut: 'verifie', verification })),
      catchError(() => of<ResultatVerificationIban>({ statut: 'erreur-technique' })),
    );
  }

  genererContrat(devisId: number, demande: DemandePaiementAuto): Observable<ResultatGenerationContrat> {
    return this.http.post<ContratAutoGenere>(`${this.url}/${devisId}/contrat`, demande).pipe(
      map((contrat): ResultatGenerationContrat => ({ statut: 'genere', contrat })),
      catchError((e: HttpErrorResponse) => of(this.resultatRefus(e))),
    );
  }

  private resultatRefus(e: HttpErrorResponse): ResultatGenerationContrat {
    if (e.status === 401) return { statut: 'session-expiree' };
    if (e.status === 422) return { statut: 'refuse', champ: e.error.champ, message: e.error.detail };
    return { statut: 'erreur-technique' };
  }
}
