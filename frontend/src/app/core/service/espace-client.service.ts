import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, of, shareReplay, tap } from 'rxjs';
import { apiBase } from '../config/api';
import { TableauDeBord } from '../model/tableau-de-bord';

/** Prénom et nom du client, pour l'en-tête de l'espace (gardés avec l'e-mail du compte qu'ils décrivent). */
export interface IdentiteEspace {
  email: string | null;
  prenom: string | null;
  nom: string | null;
}

/** US-18 : tableau de bord du client connecté. */
@Injectable({ providedIn: 'root' })
export class EspaceClientService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('assurance-iard-api')}/espace-client/tableau-de-bord`;
  private enCours: Observable<TableauDeBord | null> | null = null;

  /** Dernière identité lue ; l'en-tête ne l'affiche que pour le compte connecté (e-mail identique). */
  readonly identite = signal<IdentiteEspace | null>(null);

  /**
   * null s'il n'a pas pu être chargé. Deux demandes simultanées (page et en-tête) ne font qu'un appel.
   * @param email compte connecté, retenu avec l'identité lue
   */
  tableauDeBord(email: string | null = null): Observable<TableauDeBord | null> {
    this.enCours ??= this.http.get<TableauDeBord>(this.url).pipe(
      tap((tableau) => this.identite.set({ email, prenom: tableau.prenom, nom: tableau.nom })),
      catchError(() => of(null)),
      finalize(() => (this.enCours = null)),
      shareReplay(1),
    );
    return this.enCours;
  }
}
