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
  jourPrelevement: number | null;
  autorisationPrelevement: boolean;
}

function saisiePaiementVide(): SaisiePaiement {
  return { dateDebut: '', titulaire: '', iban: '', jourPrelevement: null, autorisationPrelevement: false };
}

export interface LigneEcheance {
  date: Date;
  montant: number;
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

  // État vérification IBAN
  protected readonly verifEnCours = signal(false);
  protected readonly verifOk = signal(false);
  protected readonly verifErreur = signal<string | null>(null);
  protected readonly ibanMasqueApi = signal<string | null>(null);
  protected readonly bic = signal('');
  protected readonly banque = signal('');

  // Affichage échéancier
  protected readonly echeancierDeplie = signal(false);

  // Envoi contrat
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

  protected readonly ibanMasqueLocal = computed(() => {
    if (!this.verifOk()) return null;
    const iban = this.saisie().iban.replace(/\s/g, '').toUpperCase();
    if (iban.length < 7) return iban;
    return iban.substring(0, 4) + ' •••• •••• •••• •••• •••• ' + iban.slice(-3);
  });

  protected readonly premiereEcheance = computed((): Date | null => {
    const s = this.saisie();
    if (!s.dateDebut || s.jourPrelevement === null || !this.verifOk()) return null;
    const dateDebut = new Date(s.dateDebut + 'T00:00:00');
    const aujourd_hui = new Date();
    aujourd_hui.setHours(0, 0, 0, 0);
    const echeancierMin = new Date(aujourd_hui);
    echeancierMin.setDate(echeancierMin.getDate() + 14);
    const auPlusTot = dateDebut > echeancierMin ? dateDebut : echeancierMin;
    const jour = s.jourPrelevement;
    const candidate = new Date(auPlusTot.getFullYear(), auPlusTot.getMonth(), jour);
    return candidate >= auPlusTot ? candidate : new Date(auPlusTot.getFullYear(), auPlusTot.getMonth() + 1, jour);
  });

  protected readonly echeancier = computed((): LigneEcheance[] => {
    const premier = this.premiereEcheance();
    const donnees = this.init();
    if (!premier || !donnees) return [];
    const jour = this.saisie().jourPrelevement!;
    const result: LigneEcheance[] = [];
    let annee = premier.getFullYear();
    let mois = premier.getMonth();
    for (let i = 0; i < 12; i++) {
      result.push({ date: new Date(annee, mois, jour), montant: donnees.prixMensuelTtc });
      mois++;
      if (mois > 11) { mois = 0; annee++; }
    }
    return result;
  });

  protected readonly echeancierVisible = computed(() =>
    this.saisie().autorisationPrelevement && this.echeancier().length > 0
  );

  protected readonly recapPaiement = computed((): string => {
    const s = this.saisie();
    if (!this.verifOk() || s.jourPrelevement === null) return 'À compléter';
    return 'Prélèvement le ' + s.jourPrelevement + ' · IBAN ' + (this.ibanMasqueApi() ?? '');
  });

  protected readonly controles = computed(() => {
    const s = this.saisie();
    const dateOk = s.dateDebut >= this.dateMin() && s.dateDebut <= this.dateMax();
    const donnees = this.init();
    let dateErreur: string | null = null;
    if (!s.dateDebut) {
      dateErreur = 'Ce champ est obligatoire.';
    } else if (!dateOk) {
      dateErreur = 'La date de début doit être comprise entre le '
        + this.formaterDateAffichage(this.dateMin())
        + ' et le ' + this.formaterDateAffichage(this.dateMax()) + '.';
    } else if (donnees?.hamonApplicable && donnees.dateFinAncienContrat) {
      const minHamon = this.lendemainDate(donnees.dateFinAncienContrat);
      if (s.dateDebut < minHamon) {
        dateErreur = 'La date de début ne peut pas être antérieure au '
          + this.formaterDateAffichage(minHamon) + ', fin de votre ancien contrat.';
      }
    }
    return new Map<string, string | null>([
      ['date-debut', dateErreur],
      ['titulaire', s.titulaire.trim().length >= 2 ? null : 'Ce champ est obligatoire.'],
      ['iban', this.verifOk() ? null : (this.saisie().iban.trim() ? 'Vérifiez votre IBAN avant de continuer.' : 'Ce champ est obligatoire.')],
      ['jour-prelevement', s.jourPrelevement !== null ? null : 'Ce champ est obligatoire.'],
      ['autorisation', s.autorisationPrelevement ? null : "Vous devez autoriser le prélèvement pour continuer."],
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
        if (res.data.hamonApplicable && res.data.dateFinAncienContrat) {
          this.saisie.update((s) => ({ ...s, dateDebut: this.lendemainDate(res.data.dateFinAncienContrat!) }));
        }
        if (res.data.numeroContratExistant) {
          if (!this.devisAuto.contratGenere()) {
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
      this.ibanMasqueApi.set(null);
      this.bic.set('');
      this.banque.set('');
    }
  }

  protected choisirJour(jour: number): void {
    this.saisie.update((s) => ({ ...s, jourPrelevement: jour }));
  }

  protected toggleAutorisation(): void {
    this.saisie.update((s) => ({ ...s, autorisationPrelevement: !s.autorisationPrelevement }));
  }

  protected onIbanBlur(): void {
    if (this.verifOk() || this.verifEnCours()) return;
    const iban = this.saisie().iban.trim();
    if (!iban) return;
    this.verifEnCours.set(true);
    this.verifErreur.set(null);
    this.service.verifierIban(iban).subscribe((res) => {
      this.verifEnCours.set(false);
      if (res.statut === 'verifie') {
        if (res.verification.valide) {
          this.verifOk.set(true);
          this.ibanMasqueApi.set(res.verification.ibanMasque);
          this.bic.set(res.verification.bic ?? '');
          this.banque.set(res.verification.banque ?? '');
        } else {
          this.verifErreur.set(res.verification.messageErreur ?? 'IBAN invalide.');
        }
      } else {
        this.verifErreur.set("Impossible de vérifier l'IBAN pour l'instant, réessayez.");
      }
    });
  }

  protected modifierIban(): void {
    this.saisie.update((s) => ({ ...s, iban: '' }));
    this.verifOk.set(false);
    this.verifErreur.set(null);
    this.ibanMasqueApi.set(null);
    this.bic.set('');
    this.banque.set('');
    setTimeout(() => this.page.querySelector<HTMLElement>('#iban')?.focus(), 0);
  }

  protected focusPaiement(): void {
    this.page.querySelector<HTMLElement>('#carte-prelevement')?.scrollIntoView({ behavior: 'smooth' });
    this.page.querySelector<HTMLElement>('#date-debut')?.focus();
  }

  protected genererContrat(e: Event): void {
    e.preventDefault();
    if (this.envoi()) return;
    this.tentative.set(true);
    this.erreurEnvoi.set(false);
    this.erreurServeur.set(null);
    const premiereErreur = [...this.controles()].find(([, msg]) => msg)?.[0];
    if (premiereErreur) {
      const el = this.page.querySelector<HTMLElement>(`#${premiereErreur}`);
      el?.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
      el?.focus();
      return;
    }
    const s = this.saisie();
    const id = this.devisAuto.devisAutoId();
    if (id === null || s.jourPrelevement === null) return;
    const demande: DemandePaiementAuto = {
      dateDebut: s.dateDebut,
      titulaire: s.titulaire.trim(),
      iban: s.iban.trim(),
      bic: this.bic() || null,
      banque: this.banque() || null,
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

  protected prixFormuleSeule(donnees: PaiementAutoInit): number {
    const sommeOptions = donnees.options.reduce((s, o) => s + (o.prixMensuelTtc ?? 0), 0);
    return donnees.prixMensuelTtc - sommeOptions;
  }

  protected formaterPrix(prix: number | null): string {
    if (prix === null) return '—';
    return prix.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
  }

  protected formaterFranchise(montant: number | null): string {
    if (montant === null) return '—';
    return montant.toLocaleString('fr-FR', { maximumFractionDigits: 0 }) + ' €';
  }

  protected formaterDate(iso: string | null): string {
    if (!iso) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  protected formaterDateObj(d: Date): string {
    return String(d.getDate()).padStart(2, '0') + '/' +
      String(d.getMonth() + 1).padStart(2, '0') + '/' + d.getFullYear();
  }

  protected formaterBonusMalus(coef: number | null): string {
    if (coef === null) return '—';
    return coef.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected formaterPrixAnnuel(donnees: PaiementAutoInit): string {
    return donnees.prixAnnuelTtc.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected recapFormule(donnees: PaiementAutoInit): string {
    const nom = this.nomFormule(donnees.formule);
    const opts = donnees.options.map((o) => this.libelleOption(o.code)).join(' + ');
    const franchise = donnees.franchise !== null
      ? '· franchise ' + this.formaterFranchise(donnees.franchise)
      : '· sans franchise';
    return opts ? `${nom} + ${opts} ${franchise}` : `${nom} ${franchise}`;
  }

  private formaterDateAffichage(isoOrDate: string): string {
    const [y, m, d] = isoOrDate.split('-');
    return `${d}/${m}/${y}`;
  }

  private lendemainDate(iso: string): string {
    const [y, m, d] = iso.split('-').map(Number);
    const date = new Date(y, m - 1, d + 1);
    return date.getFullYear() + '-' +
      String(date.getMonth() + 1).padStart(2, '0') + '-' +
      String(date.getDate()).padStart(2, '0');
  }
}
