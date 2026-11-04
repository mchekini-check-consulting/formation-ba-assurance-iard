import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';

export type ResultatLien = 'valide' | 'invalide' | 'erreur-technique';

function refusDuLien(erreur: unknown): ResultatLien {
  return erreur instanceof HttpErrorResponse && erreur.status === 410 ? 'invalide' : 'erreur-technique';
}

@Injectable({ providedIn: 'root' })
export class MotDePasseService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('backend')}/mot-de-passe`;

  /** RG1 : rien dans la réponse n'indique si le compte existe. */
  demanderLien(email: string): Observable<boolean> {
    return this.http.post<void>(`${this.url}/oubli`, { email: email.trim() }).pipe(
      map(() => true),
      catchError(() => of(false)),
    );
  }

  verifierLien(jeton: string): Observable<ResultatLien> {
    return this.http.post<void>(`${this.url}/lien`, { jeton }).pipe(
      map((): ResultatLien => 'valide'),
      catchError((erreur: unknown) => of(refusDuLien(erreur))),
    );
  }

  reinitialiser(jeton: string, motDePasse: string): Observable<ResultatLien> {
    return this.http.post<void>(`${this.url}/reinitialisation`, { jeton, motDePasse }).pipe(
      map((): ResultatLien => 'valide'),
      catchError((erreur: unknown) => of(refusDuLien(erreur))),
    );
  }
}
