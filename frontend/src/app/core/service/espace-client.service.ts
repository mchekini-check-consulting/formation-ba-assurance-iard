import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { apiBase } from '../config/api';
import { TableauDeBord } from '../model/tableau-de-bord';

/** US-18 : tableau de bord du client connecté. */
@Injectable({ providedIn: 'root' })
export class EspaceClientService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('assurance-iard-api')}/espace-client/tableau-de-bord`;

  /** null s'il n'a pas pu être chargé. */
  tableauDeBord(): Observable<TableauDeBord | null> {
    return this.http.get<TableauDeBord>(this.url).pipe(catchError(() => of(null)));
  }
}
