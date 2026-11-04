import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import { Recommandation, ReponsesBesoins } from '../model/recommandation';

@Injectable({ providedIn: 'root' })
export class RecommandationService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('backend')}/devis/recommandations`;

  /** RG4 : renvoie la référence de l'enregistrement, ou null en cas d'échec. */
  enregistrer(reponses: ReponsesBesoins, recommandation: Recommandation): Observable<string | null> {
    return this.http.post<{ reference: string }>(this.url, { ...reponses, ...recommandation }).pipe(
      map(({ reference }) => reference),
      catchError(() => of(null)),
    );
  }
}
