import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiBase } from '../config/api';

// GET /hello du backend : message de bienvenue (texte brut)
@Injectable({ providedIn: 'root' })
export class HelloService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('backend')}/hello`;

  direBonjour(): Observable<string> {
    return this.http.get(this.url, { responseType: 'text' });
  }
}
