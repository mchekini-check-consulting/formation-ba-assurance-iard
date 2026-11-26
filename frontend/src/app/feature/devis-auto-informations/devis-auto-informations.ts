import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  Civilite,
  SaisieInformationsAuto,
  dateDepuisIso,
  dateVersIso,
  erreurAdresse,
  erreurCodePostal,
  erreurDateNaissance,
  erreurTelephoneMobile,
  erreurVille,
  formaterSaisieDate,
  normaliserTelephone,
  saisieAutoVide,
} from '../../core/model/informations-auto';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { InformationsAutoService, ResultatInformationsAuto } from '../../core/service/informations-auto.service';
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

function formaterAffichageTelephone(valeur: string): string {
  const chiffres = valeur.replace(/\D/g, '').slice(0, 10);
  return chiffres.match(/.{1,2}/g)?.join(' ') ?? chiffres;
}

@Component({
  selector: 'iard-devis-auto-informations',
  standalone: true,
  imports: [DevisAutoLayout, RouterLink],
  templateUrl: './devis-auto-informations.html',
  styleUrl: './devis-auto-informations.scss',
})
export class DevisAutoInformations {
  private readonly devisAuto = inject(DevisAutoService);
  private readonly service = inject(InformationsAutoService);
  private readonly router = inject(Router);
  private readonly page = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  private readonly tentative = signal(false);

  protected readonly saisie = signal<SaisieInformationsAuto>(saisieAutoVide());
  protected readonly envoi = signal(false);
  protected readonly erreurEnvoi = signal(false);
  protected readonly erreurServeur = signal<string | null>(null);

  protected readonly telephoneAffichage = computed(() =>
    formaterAffichageTelephone(this.saisie().telephone));

  protected readonly vehiculeEncart = computed(() => {
    const v = this.devisAuto.vehicule();
    if (!v) return null;
    const a = this.devisAuto.achat();
    return {
      nom: `${v.marque} ${v.modele}`,
      plaque: a?.immatriculation ?? v.immatriculation,
    };
  });

  protected readonly formuleEncart = computed(() => this.devisAuto.formuleChoisie());

  private readonly controles = computed(() => {
    const s = this.saisie();
    return new Map<string, string | null>([
      ['civilite', s.civilite ? null : 'Indiquez votre civilité.'],
      ['nom', s.nom.trim() ? null : 'Indiquez votre nom.'],
      ['prenom', s.prenom.trim() ? null : 'Indiquez votre prénom.'],
      ['date-naissance', erreurDateNaissance(s.dateNaissance, new Date())],
      ['telephone', erreurTelephoneMobile(s.telephone)],
      ['adresse', erreurAdresse(s.adresse)],
      ['code-postal', erreurCodePostal(s.codePostal)],
      ['ville', erreurVille(s.ville)],
    ]);
  });

  constructor() {
    const id = this.devisAuto.devisAutoId();
    if (id !== null) {
      this.service.lire(id).subscribe((data) => {
        if (data) this.preRemplir(data);
      });
    }
  }

  protected erreur(id: string): string | null {
    return this.tentative() ? (this.controles().get(id) ?? null) : null;
  }

  protected choisirCivilite(civilite: Civilite): void {
    this.saisie.update((s) => ({ ...s, civilite }));
  }

  protected saisirTexte(champ: keyof SaisieInformationsAuto, e: Event): void {
    this.saisie.update((s) => ({ ...s, [champ]: (e.target as HTMLInputElement).value }));
  }

  protected saisirTelephone(e: Event): void {
    const champ = e.target as HTMLInputElement;
    const chiffres = champ.value.replace(/\D/g, '').slice(0, 10);
    champ.value = formaterAffichageTelephone(chiffres);
    this.saisie.update((s) => ({ ...s, telephone: chiffres }));
  }

  protected saisirDate(e: Event): void {
    const champ = e.target as HTMLInputElement;
    champ.value = formaterSaisieDate(champ.value);
    this.saisie.update((s) => ({ ...s, dateNaissance: champ.value }));
  }

  protected nomFormule(code: string): string {
    return NOMS_FORMULES[code] ?? code;
  }

  protected libelleOption(code: string): string {
    return LIBELLES_OPTIONS[code] ?? code;
  }

  protected franchiseMontant(code: string): number | null {
    if (code === 'INTER') return 400;
    if (code === 'TR') return 500;
    return null;
  }

  protected formaterPrix(prix: number | null): string {
    if (prix === null) return '—';
    return prix.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected continuer(e: Event): void {
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
    this.envoyer();
  }

  private preRemplir(data: { civilite?: string | null; nom?: string | null; prenom?: string | null; dateNaissance?: string | null; telephone?: string | null; adresse?: string | null; codePostal?: string | null; ville?: string | null }): void {
    this.saisie.update((s) => ({
      ...s,
      civilite: s.civilite ?? (data.civilite as Civilite | null) ?? null,
      nom: s.nom || (data.nom ?? ''),
      prenom: s.prenom || (data.prenom ?? ''),
      dateNaissance: s.dateNaissance || (data.dateNaissance ? dateDepuisIso(data.dateNaissance) : ''),
      telephone: s.telephone || (data.telephone ?? ''),
      adresse: s.adresse || (data.adresse ?? ''),
      codePostal: s.codePostal || (data.codePostal ?? ''),
      ville: s.ville || (data.ville ?? ''),
    }));
  }

  private envoyer(): void {
    const s = this.saisie();
    const id = this.devisAuto.devisAutoId();
    if (id === null) return;
    this.envoi.set(true);
    this.service
      .enregistrer(id, {
        civilite: s.civilite as string,
        nom: s.nom,
        prenom: s.prenom,
        dateNaissance: dateVersIso(s.dateNaissance),
        telephone: normaliserTelephone(s.telephone),
        adresse: s.adresse,
        codePostal: s.codePostal,
        ville: s.ville,
      })
      .subscribe((resultat) => {
        this.envoi.set(false);
        this.traiterResultat(resultat);
      });
  }

  private traiterResultat(resultat: ResultatInformationsAuto): void {
    if (resultat.statut === 'enregistre') {
      this.devisAuto.enregistrerSouscripteur();
      void this.router.navigate(['/devis-auto/paiement']);
    } else if (resultat.statut === 'session-expiree') {
      void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis-auto/informations' } });
    } else if (resultat.statut === 'refuse') {
      this.erreurServeur.set(resultat.message);
      const id = resultat.champ === 'dateNaissance' ? 'date-naissance' : resultat.champ;
      this.page.querySelector<HTMLElement>(`#${id}`)?.focus();
    } else {
      this.erreurEnvoi.set(true);
    }
  }
}
