import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { apiBase } from '../config/api';
import { FormuleAutoChoisie, PrixFormuleAutoResponse } from '../model/devis-auto';

export interface FormuleAutoSaveRequest {
  formuleChoisie: string;
  optionsChoisies: string[];
  choixDifferentDuConseil: boolean;
  versionGrille: string | null;
  prixMensuelTtc: number | null;
  prixAnnuelTtc: number | null;
}

export interface FormuleAutoSaveResponse {
  formuleChoisie: string;
  optionsChoisies: string[];
  prixMensuelTtc: number | null;
  prixAnnuelTtc: number | null;
  dateFinValidite: string | null;
}

@Injectable({ providedIn: 'root' })
export class FormuleAutoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${apiBase('assurance-iard-api')}/v1/devis/auto`;

  obtenirPrix(devisAutoId: number): Observable<PrixFormuleAutoResponse | null> {
    return this.http
      .get<PrixFormuleAutoResponse>(`${this.base}/${devisAutoId}/prix-formules`)
      .pipe(catchError(() => of(null)));
  }

  ficheConseilUrl(devisAutoId: number): string {
    return `${this.base}/${devisAutoId}/fiche-conseil`;
  }

  enregistrer(devisAutoId: number, choisie: FormuleAutoChoisie): Observable<FormuleAutoSaveResponse | null> {
    const body: FormuleAutoSaveRequest = {
      formuleChoisie: choisie.formuleMoteur,
      optionsChoisies: choisie.options,
      choixDifferentDuConseil: choisie.choixDifferentDuConseil,
      versionGrille: choisie.versionGrille,
      prixMensuelTtc: choisie.prixMensuelTtc,
      prixAnnuelTtc: choisie.prixAnnuelTtc,
    };
    return this.http
      .put<FormuleAutoSaveResponse>(`${this.base}/${devisAutoId}/formule`, body)
      .pipe(catchError(() => of(null)));
  }
}
