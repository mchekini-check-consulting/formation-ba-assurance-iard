import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PaiementAutoInit } from '../../core/model/devis-auto';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { DemandePaiementAuto, PaiementAutoService } from '../../core/service/paiement-auto.service';
import { DevisAutoLayout } from '../devis-auto-layout/devis-auto-layout';

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

interface SaisiePaiement {
  dateDebut: string;
  titulaire: string;
  iban: string;
  bic: string;
  banque: string;
  jourPrelevement: number | null;
  autorisationPrelevement: boolean;
}

function saisiePaiementVide(): SaisiePaiement {
  return { dateDebut: '', titulaire: '', iban: '', bic: '', banque: '', jourPrelevement: null, autorisationPrelevement: false };
}

@Component({
  selector: 'iard-devis-auto-paiement',
  standalone: true,
  imports: [DevisAutoLayout, RouterLink],
  templateUrl: './devis-auto-paiement.html',
  styleUrl: './devis-auto-paiement.scss',
})
export class DevisAutoPaiement {
  private readonly devisAuto = inject(DevisAutoService);
  private readonly service = inject(PaiementAutoService);
  private readonly router = inject(Router);
  private readonly page = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  protected readonly chargement = signal(true);
  protected readonly chargementErreur = signal(false);
  protected readonly init = signal<PaiementAutoInit | null>(null);
  protected readonly saisie = signal<SaisiePaiement>(saisiePaiementVide());
  protected readonly tentative = signal(false);
  protected readonly envoiVerif = signal(false);
  protected readonly verifOk = signal(false);
  protected readonly verifErreur = signal<string | null>(null);
  protected readonly ibanMasque = signal<string | null>(null);
  protected readonly envoi = signal(false);
  protected readonly erreurEnvoi = signal(false);
  protected readonly erreurServeur = signal<string | null>(null);

  protected readonly contratGenere = computed(() => this.devisAuto.contratGenere());

  protected readonly dateMin = computed(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  });

  protected readonly dateMax = computed(() => {
    const d = new Date();
    d.setDate(d.getDate() + 90);
    return d.toISOString().split('T')[0];
  });

  protected readonly controles = computed(() => {
    const s = this.saisie();
    const dateOk = s.dateDebut >= this.dateMin() && s.dateDebut <= this.dateMax();
    return new Map<string, string | null>([
      ['date-debut', s.dateDebut ? (dateOk ? null : 'La date doit être entre demain et 90 jours.') : 'Choisissez une date de début.'],
      ['titulaire', s.titulaire.trim().length >= 2 ? null : 'Indiquez le titulaire du compte (au moins 2 caractères).'],
      ['iban', this.verifOk() ? null : 'Vérifiez votre IBAN avant de continuer.'],
      ['jour-prelevement', s.jourPrelevement !== null ? null : 'Choisissez un jour de prélèvement.'],
      ['autorisation', s.autorisationPrelevement ? null : 'Vous devez autoriser le prélèvement pour continuer.'],
    ]);
  });

  constructor() {
    const id = this.devisAuto.devisAutoId();
    if (id === null) {
      void this.router.navigate(['/devis-auto/informations']);
      return;
    }
    this.service.charger(id).subscribe((res) => {
      this.chargement.set(false);
      if (res.statut === 'charge') {
        this.init.set(res.data);
        this.saisie.update((s) => ({ ...s, titulaire: res.data.titulairePrerempli }));
        if (res.data.numeroContratExistant) {
          const contratExistant = this.devisAuto.contratGenere();
          if (!contratExistant) {
            this.devisAuto.enregistrerContrat({
              numero: res.data.numeroContratExistant,
              statut: 'EN_ATTENTE_DE_SIGNATURE',
              documentMandatSepaId: res.data.documentMandatSepaIdExistant,
              premierPrelevement: null,
              montantMensuel: null,
            });
          }
        }
      } else if (res.statut === 'session-expiree') {
        void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis-auto/paiement' } });
      } else {
        this.chargementErreur.set(true);
      }
    });
  }

  protected erreur(id: string): string | null {
    return this.tentative() ? (this.controles().get(id) ?? null) : null;
  }

  protected saisirTexte(champ: keyof SaisiePaiement, e: Event): void {
    const valeur = (e.target as HTMLInputElement).value;
    this.saisie.update((s) => ({ ...s, [champ]: valeur }));
    if (champ === 'iban') {
      this.verifOk.set(false);
      this.verifErreur.set(null);
      this.ibanMasque.set(null);
      this.saisie.update((s) => ({ ...s, bic: '', banque: '' }));
    }
  }

  protected choisirJour(jour: number): void {
    this.saisie.update((s) => ({ ...s, jourPrelevement: jour }));
  }

  protected toggleAutorisation(): void {
    this.saisie.update((s) => ({ ...s, autorisationPrelevement: !s.autorisationPrelevement }));
  }

  protected verifierIban(e: Event): void {
    e.preventDefault();
    const iban = this.saisie().iban.trim();
    if (!iban) return;
    this.envoiVerif.set(true);
    this.verifOk.set(false);
    this.verifErreur.set(null);
    this.service.verifierIban(iban).subscribe((res) => {
      this.envoiVerif.set(false);
      if (res.statut === 'verifie') {
        if (res.verification.valide) {
          this.verifOk.set(true);
          this.ibanMasque.set(res.verification.ibanMasque);
          this.saisie.update((s) => ({
            ...s,
            bic: res.verification.bic ?? '',
            banque: res.verification.banque ?? '',
          }));
        } else {
          this.verifErreur.set(res.verification.messageErreur ?? 'IBAN invalide.');
        }
      } else {
        this.verifErreur.set('Impossible de vérifier l\'IBAN pour l\'instant, réessayez.');
      }
    });
  }

  protected genererContrat(e: Event): void {
    e.preventDefault();
    if (this.envoi()) return;
    this.tentative.set(true);
    this.erreurEnvoi.set(false);
    this.erreurServeur.set(null);
    const premiereErreur = [...this.controles()].find(([, msg]) => msg)?.[0];
    if (premiereErreur) {
      this.page.querySelector<HTMLElement>(`#${premiereErreur}`)?.focus();
      return;
    }
    const s = this.saisie();
    const id = this.devisAuto.devisAutoId();
    if (id === null || s.jourPrelevement === null) return;
    const demande: DemandePaiementAuto = {
      dateDebut: s.dateDebut,
      titulaire: s.titulaire.trim(),
      iban: s.iban.trim(),
      bic: s.bic.trim(),
      banque: s.banque.trim(),
      jourPrelevement: s.jourPrelevement,
      autorisationPrelevement: true,
    };
    this.envoi.set(true);
    this.service.genererContrat(id, demande).subscribe((res) => {
      this.envoi.set(false);
      if (res.statut === 'genere') {
        this.devisAuto.enregistrerContrat(res.contrat);
      } else if (res.statut === 'session-expiree') {
        void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis-auto/paiement' } });
      } else if (res.statut === 'refuse') {
        this.erreurServeur.set(res.message);
        const idChamp = res.champ === 'dateDebut' ? 'date-debut' : res.champ === 'jourPrelevement' ? 'jour-prelevement' : res.champ;
        this.page.querySelector<HTMLElement>(`#${idChamp}`)?.focus();
      } else {
        this.erreurEnvoi.set(true);
      }
    });
  }

  protected nomFormule(code: string): string {
    return NOMS_FORMULES[code] ?? code;
  }

  protected libelleOption(code: string): string {
    return LIBELLES_OPTIONS[code] ?? code;
  }

  protected formaterPrix(prix: number | null): string {
    if (prix === null) return '—';
    return prix.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
  }

  protected formaterDate(iso: string | null): string {
    if (!iso) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }
}
