import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import { FactureGeneree, MesFactures, PeriodeFacture } from '../model/facture';

export type ResultatFacture =
  | { statut: 'genere'; generation: FactureGeneree }
  /** 422 : contrat en attente de signature, ou période après la fin d'un contrat Résilié ou Renoncé. */
  | { statut: 'impossible'; message: string }
  /** 404 : l'échéance ou l'année n'existe plus (échéancier modifié entre-temps). */
  | { statut: 'introuvable'; message: string }
  | { statut: 'erreur-technique' };

export const MESSAGE_FACTURE_IMPOSSIBLE = 'Aucune facture ne peut être générée pour cette période de ce contrat.';
export const MESSAGE_PERIODE_INCONNUE =
  "Cette échéance n'est plus dans votre échéancier. Rechargez la page pour voir l'échéancier à jour.";
export const MESSAGE_ERREUR_FACTURE = "La facture n'a pas pu être générée. Réessayez dans quelques instants.";

/**
 * US-22 : « Mes factures » (GET /espace-client/factures) et génération d'une facture
 * (POST /contrats/{numero}/factures). Un refus (403) affiche la page « Accès non autorisé » (intercepteur d'US-04).
 */
@Injectable({ providedIn: 'root' })
export class FacturesService {
  private readonly http = inject(HttpClient);

  /** AC1, AC4 : null si les factures n'ont pas pu être chargées. */
  lister(): Observable<MesFactures | null> {
    return this.http
      .get<MesFactures>(`${apiBase('assurance-iard-api')}/espace-client/factures`)
      .pipe(catchError(() => of(null)));
  }

  /** AC2 : génère la facture de cette échéance (sa date) ou de cette année (sa date de début). */
  generer(numeroContrat: string, periode: PeriodeFacture, date: string): Observable<ResultatFacture> {
    return this.http
      .post<FactureGeneree>(
        `${apiBase('assurance-iard-api')}/contrats/${encodeURIComponent(numeroContrat)}/factures`,
        { periode, date },
      )
      .pipe(
        map((generation): ResultatFacture => ({ statut: 'genere', generation })),
        catchError((erreur: HttpErrorResponse) => {
          if (erreur.status === 422) {
            const detail = (erreur.error as { detail?: string } | null)?.detail;
            return of<ResultatFacture>({ statut: 'impossible', message: detail ?? MESSAGE_FACTURE_IMPOSSIBLE });
          }
          if (erreur.status === 404) {
            return of<ResultatFacture>({ statut: 'introuvable', message: MESSAGE_PERIODE_INCONNUE });
          }
          return of<ResultatFacture>({ statut: 'erreur-technique' });
        }),
      );
  }
}
