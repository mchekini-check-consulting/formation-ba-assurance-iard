import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { apiBase } from '../config/api';
import { DocumentClient } from '../model/document';

/** US-04 : « Mes documents ». Un refus (403) est affiché par la page « Accès non autorisé ». */
@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('assurance-iard-api')}/documents`;

  /** null si la liste n'a pas pu être chargée. */
  lister(): Observable<DocumentClient[] | null> {
    return this.http.get<DocumentClient[]>(this.url).pipe(catchError(() => of(null)));
  }

  /** Contenu du document ; null s'il n'a pas pu être ouvert. */
  ouvrir(id: string): Observable<Blob | null> {
    return this.http.get(`${this.url}/${id}`, { responseType: 'blob' }).pipe(catchError(() => of(null)));
  }
}
