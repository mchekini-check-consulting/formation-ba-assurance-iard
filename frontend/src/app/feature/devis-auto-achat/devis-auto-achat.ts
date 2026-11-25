import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AchatAuto, ModeAchat, SivRechercheResponse, TitulaireCarteGrise, VehiculeAuto } from '../../core/model/devis-auto';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { SivService } from '../../core/service/siv.service';
import { DevisAutoLayout } from '../devis-auto-layout/devis-auto-layout';

function validerDate(str: string, anneeVehicule: number | null): string | null {
  if (!str.trim()) return "Indiquez la date d'acquisition.";
  if (!/^\d{2}\/\d{2}\/\d{4}$/.test(str)) return 'Format attendu : JJ/MM/AAAA.';
  const [j, m, a] = str.split('/').map(Number);
  const date = new Date(a, m - 1, j);
  if (date.getDate() !== j || date.getMonth() !== m - 1 || date.getFullYear() !== a) {
    return "La date d'acquisition n'est pas valide.";
  }
  if (date > new Date()) return "La date d'acquisition ne peut pas être dans le futur.";
  if (anneeVehicule !== null && a < anneeVehicule) return "La date d'acquisition n'est pas valide.";
  return null;
}

function validerFormatImmatriculation(str: string): string | null {
  if (!str) return null; // facultatif : vide accepté
  if (!/^[A-Z]{2}-\d{3}-[A-Z]{2}$/.test(str)) return 'Format attendu : AB-123-CD.';
  return null;
}

@Component({
  selector: 'iard-devis-auto-achat',
  imports: [RouterLink, DevisAutoLayout],
  templateUrl: './devis-auto-achat.html',
  styleUrl: './devis-auto-achat.scss',
})
export class DevisAutoAchat {
  private readonly router = inject(Router);
  private readonly sivService = inject(SivService);
  protected readonly devisAutoService = inject(DevisAutoService);
  protected readonly vehicule: VehiculeAuto = this.devisAutoService.vehicule()!;
  protected readonly modePlaque: boolean = this.vehicule.immatriculation !== null;

  protected readonly vehiculeConfirme = signal<boolean | null>(
    this.modePlaque && this.devisAutoService.achat() !== null ? true : null,
  );
  protected readonly nonImmatricule = signal<boolean>(
    this.devisAutoService.achat()?.nonImmatricule ?? false,
  );
  protected readonly immatriculationSaisie = signal<string>(
    this.devisAutoService.achat()?.immatriculation ?? '',
  );
  protected readonly verificationSiv = signal<SivRechercheResponse | null>(null);
  protected readonly verificationEnCours = signal(false);
  protected readonly dateAcquisition = signal<string>(
    this.devisAutoService.achat()?.dateAcquisition ?? '',
  );
  protected readonly modeAchat = signal<ModeAchat | null>(
    this.devisAutoService.achat()?.modeAchat ?? null,
  );
  protected readonly titulaireCarteGrise = signal<TitulaireCarteGrise | null>(
    this.devisAutoService.achat()?.titulaireCarteGrise ?? null,
  );
  protected readonly tentative = signal(false);

  protected readonly erreurVehicule = computed(() => {
    if (!this.tentative() || !this.modePlaque) return null;
    if (this.vehiculeConfirme() === null) return 'Confirmez si ce véhicule est le vôtre.';
    if (this.vehiculeConfirme() === false) return 'Retrouvez votre véhicule par sa marque pour continuer.';
    return null;
  });

  protected readonly erreurImmatriculation = computed(() => {
    if (!this.tentative() || this.modePlaque || this.nonImmatricule()) return null;
    return validerFormatImmatriculation(this.immatriculationSaisie());
  });

  protected readonly erreurDate = computed(() =>
    this.tentative() ? validerDate(this.dateAcquisition(), this.vehicule.annee) : null,
  );

  protected readonly erreurModeAchat = computed(() =>
    this.tentative() && !this.modeAchat() ? "Indiquez votre mode d'achat." : null,
  );

  protected readonly erreurTitulaire = computed(() =>
    this.tentative() && !this.titulaireCarteGrise() ? 'Indiquez le titulaire de la carte grise.' : null,
  );

  protected readonly avertissementModele = computed(() => {
    const siv = this.verificationSiv();
    if (!siv || siv.statut !== 'TROUVE') return null;
    const memeVehicule =
      siv.modele?.toLowerCase() === this.vehicule.modele.toLowerCase() &&
      siv.marque?.toLowerCase() === this.vehicule.marque.toLowerCase();
    if (memeVehicule) return null;
    return `${siv.marque ?? ''} ${siv.modele ?? ''}`.trim();
  });

  protected readonly modesAchat: { valeur: ModeAchat; libelle: string; titre?: string }[] = [
    { valeur: 'COMPTANT', libelle: 'Comptant' },
    { valeur: 'CREDIT', libelle: 'Crédit' },
    { valeur: 'LOA', libelle: 'LOA', titre: "Location avec option d'achat" },
    { valeur: 'LLD', libelle: 'LLD', titre: 'Location longue durée' },
  ];

  protected readonly titulaires: { valeur: TitulaireCarteGrise; libelle: string }[] = [
    { valeur: 'SOUSCRIPTEUR', libelle: 'À mon nom' },
    { valeur: 'CONJOINT', libelle: 'Mon conjoint' },
    { valeur: 'ENFANT', libelle: 'Mon enfant' },
    { valeur: 'SOCIETE_LOCATION', libelle: 'Société de location' },
    { valeur: 'AUTRE_PERSONNE', libelle: 'Une autre personne' },
  ];

  protected formaterEnergie(energie: string): string {
    if (!energie) return energie;
    return energie.charAt(0).toUpperCase() + energie.slice(1).toLowerCase();
  }

  protected toggleNonImmatricule(): void {
    const val = !this.nonImmatricule();
    this.nonImmatricule.set(val);
    if (val) {
      this.immatriculationSaisie.set('');
      this.verificationSiv.set(null);
    }
  }

  protected formaterImmatriculation(e: Event): void {
    const raw = (e.target as HTMLInputElement).value.toUpperCase().replace(/[^A-Z0-9]/g, '');
    let formatted = raw.slice(0, 2);
    if (raw.length > 2) formatted += '-' + raw.slice(2, Math.min(5, raw.length));
    if (raw.length > 5) formatted += '-' + raw.slice(5, Math.min(7, raw.length));
    (e.target as HTMLInputElement).value = formatted;
    this.immatriculationSaisie.set(formatted);
    this.verificationSiv.set(null);
  }

  protected verifierPlaque(): void {
    const val = this.immatriculationSaisie();
    if (!val || !/^[A-Z]{2}-\d{3}-[A-Z]{2}$/.test(val)) return;
    this.verificationEnCours.set(true);
    this.verificationSiv.set(null);
    this.sivService.chercherParPlaque(val).subscribe(rep => {
      this.verificationEnCours.set(false);
      this.verificationSiv.set(rep);
    });
  }

  protected saisirDate(e: Event): void {
    const raw = (e.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 8);
    let formatted = raw.slice(0, 2);
    if (raw.length > 2) formatted += '/' + raw.slice(2, 4);
    if (raw.length > 4) formatted += '/' + raw.slice(4, 8);
    (e.target as HTMLInputElement).value = formatted;
    this.dateAcquisition.set(formatted);
  }

  protected continuer(e: Event): void {
    e.preventDefault();
    this.tentative.set(true);
    const plaqueOk = !this.modePlaque || this.vehiculeConfirme() === true;
    const immatOk = this.modePlaque || this.nonImmatricule() || !validerFormatImmatriculation(this.immatriculationSaisie());
    const dateOk = !validerDate(this.dateAcquisition(), this.vehicule.annee);
    const achatOk = !!this.modeAchat();
    const titulaireOk = !!this.titulaireCarteGrise();
    if (!plaqueOk || !immatOk || !dateOk || !achatOk || !titulaireOk) {
      setTimeout(() => {
        const el = document.querySelector<HTMLElement>('[role="alert"]');
        el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 0);
      return;
    }
    const immatriculation = this.modePlaque ? null : (this.immatriculationSaisie() || null);
    const achat: AchatAuto = {
      immatriculation,
      plaqueRenseignee: this.modePlaque ? true : !!this.immatriculationSaisie(),
      nonImmatricule: !this.modePlaque && this.nonImmatricule(),
      dateAcquisition: this.dateAcquisition(),
      modeAchat: this.modeAchat()!,
      titulaireCarteGrise: this.titulaireCarteGrise()!,
    };
    this.devisAutoService.enregistrerAchat(achat);
    void this.router.navigate(['/devis-auto/usage']);
  }
}
