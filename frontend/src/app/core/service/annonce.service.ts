import { Injectable } from '@angular/core';

/** Message à afficher une seule fois sur l'accueil, par exemple après une déconnexion. */
@Injectable({ providedIn: 'root' })
export class AnnonceService {
  private message: string | null = null;

  preparer(message: string): void {
    this.message = message;
  }

  prendre(): string | null {
    const message = this.message;
    this.message = null;
    return message;
  }
}
