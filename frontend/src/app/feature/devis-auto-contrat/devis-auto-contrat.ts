import { DatePipe, DecimalPipe, TitleCasePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContratAutoDetail } from '../../core/model/devis-auto';
import { ContratAutoService, ErreurAvecEssais } from '../../core/service/contrat-auto.service';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { BadgeStatut } from '../badge-statut/badge-statut';
import { DevisAutoLayout } from '../devis-auto-layout/devis-auto-layout';

function abrevierCivilite(civ: string | null | undefined): string {
  if (civ === 'madame') return 'Mme';
  if (civ === 'monsieur') return 'M.';
  return civ ?? '';
}

const NOMS_FORMULES: Record<string, string> = {
  TIERS: 'Tiers',
  INTER: 'Intermédiaire',
  TR: 'Tous risques',
};

const LIBELLES_OPTIONS: Record<string, string> = {
  ASSISTANCE_0KM: 'Assistance dès 0 km',
  VEHICULE_REMPLACEMENT: 'Véhicule de remplacement',
  PROTECTION_CONDUCTEUR: 'Protection du conducteur renforcée',
  PROTECTION_JURIDIQUE: 'Protection juridique',
};

type Etape = 'chargement' | 'erreur-chargement' | 'lecture' | 'code-envoye' | 'signe';

@Component({
  selector: 'iard-devis-auto-contrat',
  standalone: true,
  imports: [DevisAutoLayout, RouterLink, DecimalPipe, DatePipe, TitleCasePipe, BadgeStatut],
  templateUrl: './devis-auto-contrat.html',
  styleUrl: './devis-auto-contrat.scss',
})
export class DevisAutoContrat implements OnInit {
  private readonly devisAutoSvc = inject(DevisAutoService);
  private readonly service = inject(ContratAutoService);

  protected readonly etape = signal<Etape>('chargement');
  protected readonly contrat = signal<ContratAutoDetail | null>(null);
  protected readonly cgAcceptees = signal(false);
  protected readonly emailMasque = signal<string | null>(null);
  protected readonly code = signal('');
  protected readonly envoi = signal(false);
  protected readonly messageErreur = signal<string | null>(null);
  protected readonly essaisRestants = signal<number | null>(null);
  protected readonly contratSigne = signal<ContratAutoDetail | null>(null);
  protected readonly nomFormule = NOMS_FORMULES;
  protected readonly libOption = LIBELLES_OPTIONS;

  ngOnInit(): void {
    const id = this.devisAutoSvc.devisAutoId();
    if (!id) {
      this.etape.set('erreur-chargement');
      return;
    }
    this.service.obtenir(id).subscribe({
      next: (c) => {
        this.contrat.set(c);
        if (c.statut !== 'EN_ATTENTE_DE_SIGNATURE') {
          this.contratSigne.set(c);
          this.etape.set('signe');
        } else {
          this.etape.set('lecture');
        }
      },
      error: () => this.etape.set('erreur-chargement'),
    });
  }

  protected envoyerCode(): void {
    const id = this.devisAutoSvc.devisAutoId();
    if (!id) return;
    this.envoi.set(true);
    this.messageErreur.set(null);
    this.service.envoyerCode(id).subscribe({
      next: (r) => {
        this.emailMasque.set(r.emailMasque);
        this.etape.set('code-envoye');
        this.envoi.set(false);
      },
      error: (e: ErreurAvecEssais) => {
        this.envoi.set(false);
        this.afficherErreur(e);
      },
    });
  }

  protected renvoyerCode(): void {
    const id = this.devisAutoSvc.devisAutoId();
    if (!id) return;
    this.messageErreur.set(null);
    this.service.renvoyerCode(id).subscribe({
      next: (r) => {
        this.emailMasque.set(r.emailMasque);
        this.code.set('');
        this.messageErreur.set(null);
      },
      error: (e: ErreurAvecEssais) => this.afficherErreur(e),
    });
  }

  protected signer(): void {
    const id = this.devisAutoSvc.devisAutoId();
    if (!id || !this.code()) return;
    this.envoi.set(true);
    this.messageErreur.set(null);
    this.service.signer(id, this.code()).subscribe({
      next: (c) => {
        this.contratSigne.set(c);
        this.etape.set('signe');
        this.envoi.set(false);
      },
      error: (e: ErreurAvecEssais) => {
        this.envoi.set(false);
        this.afficherErreur(e);
      },
    });
  }

  protected mettreAJourCode(event: Event): void {
    this.code.set((event.target as HTMLInputElement).value);
  }

  protected formuleLibelle(code: string | null): string {
    if (!code) return '';
    return NOMS_FORMULES[code] ?? code;
  }

  protected optionLibelle(code: string): string {
    return LIBELLES_OPTIONS[code] ?? code;
  }

  protected abrevierCivilite(civ: string | null): string {
    return abrevierCivilite(civ);
  }

  private afficherErreur(e: ErreurAvecEssais): void {
    switch (e.code) {
      case 'session-expiree':
        this.messageErreur.set('Votre session a expiré. Veuillez vous reconnecter.');
        break;
      case 'code-expire':
        this.messageErreur.set('Ce code a expiré. Cliquez sur « Renvoyer un code » pour en recevoir un nouveau.');
        break;
      case 'essais-epuises':
        this.messageErreur.set('Vous avez épuisé vos tentatives. Cliquez sur « Renvoyer un code » pour en recevoir un nouveau.');
        break;
      case 'code-incorrect':
        this.essaisRestants.set(e.essaisRestants ?? null);
        this.messageErreur.set(
          e.essaisRestants != null
            ? `Code incorrect. Il vous reste ${e.essaisRestants} tentative${e.essaisRestants > 1 ? 's' : ''}.`
            : 'Code incorrect.'
        );
        break;
      default:
        this.messageErreur.set('Une erreur est survenue. Veuillez réessayer.');
    }
  }
}
