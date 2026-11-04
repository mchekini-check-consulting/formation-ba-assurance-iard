import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, fromEvent, interval, merge } from 'rxjs';
import { AnnonceService } from './annonce.service';
import { ConnexionService } from './connexion.service';

export const DELAI_INACTIVITE_MS = 30 * 60 * 1000;
export const DELAI_PROLONGATION_MS = 5 * 60 * 1000;
const INTERVALLE_CONTROLE_MS = 30 * 1000;
const EVENEMENTS_ACTIVITE = ['click', 'keydown', 'scroll', 'touchstart'];
const MESSAGE_INACTIVITE = 'Vous avez été déconnecté après 30 minutes d’inactivité.';

/**
 * RG3 de l'US-07 : déconnexion après 30 minutes sans activité. Tant que le client est actif, la session est
 * prolongée auprès du backend ; si le poste était en veille, le backend la refuse de toute façon.
 */
@Injectable({ providedIn: 'root' })
export class InactiviteService {
  private readonly connexionService = inject(ConnexionService);
  private readonly annonceService = inject(AnnonceService);
  private readonly router = inject(Router);

  private surveillance?: Subscription;
  private derniereActivite = 0;
  private derniereProlongation = 0;

  surveiller(): void {
    if (this.surveillance) {
      return;
    }
    this.derniereActivite = Date.now();
    this.derniereProlongation = this.derniereActivite;
    const activite = merge(
      ...EVENEMENTS_ACTIVITE.map((evenement) => fromEvent(document, evenement, { capture: true, passive: true })),
    );
    const controle = merge(interval(INTERVALLE_CONTROLE_MS), fromEvent(document, 'visibilitychange'));
    this.surveillance = activite.subscribe(() => this.enregistrerActivite());
    this.surveillance.add(controle.subscribe(() => this.controler()));
  }

  private arreter(): void {
    this.surveillance?.unsubscribe();
    this.surveillance = undefined;
  }

  private inactifDepuisTrop(): boolean {
    return Date.now() - this.derniereActivite >= DELAI_INACTIVITE_MS;
  }

  private controler(): void {
    if (!this.connexionService.jeton()) {
      this.arreter();
    } else if (this.inactifDepuisTrop()) {
      this.deconnecter();
    }
  }

  private enregistrerActivite(): void {
    if (this.inactifDepuisTrop()) {
      this.deconnecter();
      return;
    }
    const maintenant = Date.now();
    this.derniereActivite = maintenant;
    if (maintenant - this.derniereProlongation >= DELAI_PROLONGATION_MS) {
      this.derniereProlongation = maintenant;
      this.prolonger();
    }
  }

  private prolonger(): void {
    this.connexionService.sessionValide().subscribe((valide) => {
      if (!valide && !this.connexionService.jeton()) {
        this.deconnecter();
      }
    });
  }

  private deconnecter(): void {
    this.arreter();
    this.connexionService.deconnecter().subscribe(() => {
      this.annonceService.preparer(MESSAGE_INACTIVITE);
      void this.router.navigate(['/']);
    });
  }
}
