import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { apiBase } from '../config/api';
import { Compte, DemandeCreationCompte, ResultatCreationCompte } from '../model/compte';

@Injectable({ providedIn: 'root' })
export class CompteService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('backend')}/comptes`;

  readonly emailConnecte = signal<string | null>(null);

  creerCompte(demande: DemandeCreationCompte): Observable<ResultatCreationCompte> {
    return this.http.post<Compte>(this.url, { email: demande.email.trim(), motDePasse: demande.motDePasse }).pipe(
      tap((compte) => this.emailConnecte.set(compte.email)),
      map((): ResultatCreationCompte => 'cree'),
      catchError((erreur: unknown) =>
        of<ResultatCreationCompte>(
          erreur instanceof HttpErrorResponse && erreur.status === 409 ? 'email-deja-utilise' : 'erreur-technique',
        ),
      ),
    );
  }
}
