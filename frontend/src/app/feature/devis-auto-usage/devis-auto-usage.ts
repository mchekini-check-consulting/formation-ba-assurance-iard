import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { KmAnnuels, StationnementAuto, UsageAuto, UsageAutoSaisie } from '../../core/model/devis-auto';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { DevisAutoLayout } from '../devis-auto-layout/devis-auto-layout';

function validerCodePostal(str: string): string | null {
  if (!str.trim()) return 'Saisissez un code postal.';
  if (!/^\d{5}$/.test(str)) return 'Code postal invalide (5 chiffres).';
  return null;
}

@Component({
  selector: 'iard-devis-auto-usage',
  imports: [DevisAutoLayout, RouterLink],
  templateUrl: './devis-auto-usage.html',
  styleUrl: './devis-auto-usage.scss',
})
export class DevisAutoUsage {
  private readonly router = inject(Router);
  private readonly devisAutoService = inject(DevisAutoService);

  private readonly saisieExistante = this.devisAutoService.usage();

  protected readonly usage = signal<UsageAuto | null>(this.saisieExistante?.usage ?? null);
  protected readonly kmAnnuels = signal<KmAnnuels | null>(this.saisieExistante?.kmAnnuels ?? null);
  protected readonly stationnement = signal<StationnementAuto | null>(
    this.saisieExistante?.stationnement ?? null,
  );
  protected readonly codePostal = signal<string>(this.saisieExistante?.codePostal ?? '');
  protected readonly tentative = signal(false);

  protected readonly erreurUsage = computed(() =>
    this.tentative() && !this.usage() ? "Indiquez l'usage de votre véhicule." : null,
  );
  protected readonly erreurKm = computed(() =>
    this.tentative() && !this.kmAnnuels() ? 'Choisissez votre kilométrage annuel.' : null,
  );
  protected readonly erreurStationnement = computed(() =>
    this.tentative() && !this.stationnement()
      ? 'Indiquez où stationne votre véhicule la nuit.'
      : null,
  );
  protected readonly erreurCodePostal = computed(() =>
    this.tentative() ? validerCodePostal(this.codePostal()) : null,
  );

  protected readonly usages: { valeur: UsageAuto; libelle: string }[] = [
    { valeur: 'PRIVE', libelle: 'Vie privée' },
    { valeur: 'TRAJET_TRAVAIL', libelle: 'Privé et trajet travail' },
    { valeur: 'PROFESSIONNEL', libelle: 'Usage professionnel' },
  ];

  protected readonly tranchesKm: { valeur: KmAnnuels; libelle: string }[] = [
    { valeur: 'MOINS_8000', libelle: 'Moins de 8 000 km' },
    { valeur: '8000_15000', libelle: '8 000 à 15 000 km' },
    { valeur: '15001_25000', libelle: '15 001 à 25 000 km' },
    { valeur: 'PLUS_25000', libelle: 'Plus de 25 000 km' },
  ];

  protected readonly stationnements: { valeur: StationnementAuto; libelle: string }[] = [
    { valeur: 'GARAGE_FERME', libelle: 'Garage fermé' },
    { valeur: 'PARKING_PRIVE', libelle: 'Parking privé' },
    { valeur: 'RUE', libelle: 'Voie publique' },
  ];

  protected choisirKm(e: Event): void {
    const valeur = (e.target as HTMLSelectElement).value as KmAnnuels | '';
    this.kmAnnuels.set(valeur || null);
  }

  protected saisirCodePostal(e: Event): void {
    const valeur = (e.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 5);
    (e.target as HTMLInputElement).value = valeur;
    this.codePostal.set(valeur);
  }

  protected continuer(e: Event): void {
    e.preventDefault();
    this.tentative.set(true);
    const ok =
      !!this.usage() &&
      !!this.kmAnnuels() &&
      !!this.stationnement() &&
      !validerCodePostal(this.codePostal());
    if (!ok) {
      setTimeout(() => {
        const el = document.querySelector<HTMLElement>('[role="alert"]');
        el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 0);
      return;
    }
    const saisie: UsageAutoSaisie = {
      usage: this.usage()!,
      kmAnnuels: this.kmAnnuels()!,
      stationnement: this.stationnement()!,
      codePostal: this.codePostal(),
    };
    this.devisAutoService.enregistrerUsage(saisie);
    void this.router.navigate(['/devis-auto/conducteur']);
  }
}
