import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { apiBase } from '../config/api';
import { AccesRefuse, ClientResume, DossierClient } from '../model/back-office';

/** US-04 : back-office (dossiers clients, journal des accès refusés). null : données non chargées. */
@Injectable({ providedIn: 'root' })
export class BackOfficeService {
  private readonly http = inject(HttpClient);
  private readonly url = apiBase('assurance-iard-api');

  listerClients(): Observable<ClientResume[] | null> {
    return this.http.get<ClientResume[]>(`${this.url}/back-office/clients`).pipe(catchError(() => of(null)));
  }

  dossier(id: string): Observable<DossierClient | null> {
    return this.http.get<DossierClient>(`${this.url}/back-office/clients/${id}`).pipe(catchError(() => of(null)));
  }

  accesRefuses(): Observable<AccesRefuse[] | null> {
    return this.http.get<AccesRefuse[]>(`${this.url}/admin/acces-refuses`).pipe(catchError(() => of(null)));
  }
}
