import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { apiBase } from '../config/api';

export interface TarifAutoRequete {
  refVehiculeId?: number;
  immatriculation?: string;
  anneePermis: number;
  bonusMalus: number;
  kmAnnuels: number;
  usage: string;
  stationnement: string;
  formule?: string;
}

export interface TarifAutoPrix {
  formule: string;
  mensuel: number;
}

export interface TarifAutoReponse {
  tarifable: boolean;
  motif?: string;
  prix: TarifAutoPrix[];
}

@Injectable({ providedIn: 'root' })
export class TarifAutoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${apiBase('assurance-iard-api')}/v1/devis/auto`;

  calculer(requete: TarifAutoRequete): Observable<TarifAutoReponse | null> {
    return this.http
      .post<TarifAutoReponse>(`${this.base}/tarif`, requete)
      .pipe(catchError(() => of(null)));
  }
}
