import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { apiBase } from '../config/api';
import { DemandeTarif, Tarif } from '../model/tarif';

@Injectable({ providedIn: 'root' })
export class TarifService {
  private readonly http = inject(HttpClient);
  private readonly base = `${apiBase('assurance-iard-api')}/devis`;

  estimer(demande: DemandeTarif): Observable<Tarif | null> {
    return this.http.post<Tarif>(`${this.base}/tarifs`, demande).pipe(catchError(() => of(null)));
  }

  tarifer(reference: string, demande: DemandeTarif): Observable<Tarif | null> {
    return this.http.post<Tarif>(`${this.base}/${reference}/tarif`, demande).pipe(catchError(() => of(null)));
  }

  tarifRetenu(reference: string): Observable<Tarif | null> {
    return this.http.get<Tarif>(`${this.base}/${reference}/tarif`).pipe(catchError(() => of(null)));
  }
}
