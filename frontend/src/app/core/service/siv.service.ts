import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { apiBase } from '../config/api';
import { CriteresEligibilite, MarqueVehicule, SivRechercheResponse } from '../model/devis-auto';

@Injectable({ providedIn: 'root' })
export class SivService {
  private readonly http = inject(HttpClient);
  private readonly base = `${apiBase('assurance-iard-api')}/v1/devis/auto`;

  chercherParPlaque(immatriculation: string): Observable<SivRechercheResponse | null> {
    return this.http
      .post<SivRechercheResponse>(`${this.base}/recherche-vehicule`, { immatriculation })
      .pipe(catchError(() => of(null)));
  }

  referentielVehicules(): Observable<MarqueVehicule[]> {
    return this.http.get<MarqueVehicule[]>(`${this.base}/referentiels/vehicules`).pipe(catchError(() => of([])));
  }

  criteresEligibilite(): Observable<CriteresEligibilite | null> {
    return this.http
      .get<CriteresEligibilite>(`${this.base}/referentiels/criteres-eligibilite`)
      .pipe(catchError(() => of(null)));
  }
}
