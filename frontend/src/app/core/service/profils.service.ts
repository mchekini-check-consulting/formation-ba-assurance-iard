import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiBase } from '../config/api';

export interface Permission {
  rubrique: string;
  droit: string;
  valeur: boolean;
}

export interface ProfilInfo {
  nom: string;
  description: string;
  nombreUtilisateurs: number;
  permissions: Permission[];
}

export interface HistoriqueEntree {
  modifieLe: string;
  rubrique: string;
  droit: string;
  ancienneValeur: boolean;
  nouvelleValeur: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProfilsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('assurance-iard-api')}/admin/profils`;

  lister(): Observable<ProfilInfo[]> {
    return this.http.get<ProfilInfo[]>(this.url);
  }

  mettreAJourPermissions(profil: string, permissions: Permission[]): Observable<void> {
    return this.http.put<void>(`${this.url}/${profil}/permissions`, { permissions });
  }

  lireHistorique(profil: string): Observable<HistoriqueEntree[]> {
    return this.http.get<HistoriqueEntree[]>(`${this.url}/${profil}/historique`);
  }
}
