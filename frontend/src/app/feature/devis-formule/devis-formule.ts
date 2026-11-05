import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  AJUSTEMENTS_FRANCHISE,
  FORMULES,
  Formule,
  Franchise,
  LIBELLES_OPTIONS,
  OptionConseillee,
  PRIX_OPTIONS,
  RANG_FORMULE,
  formaterPrix,
  prixFormule,
} from '../../core/model/recommandation';
import { DevisService } from '../../core/service/devis.service';
import { DevisEntete } from '../devis-entete/devis-entete';

const ORDRE: Formule[] = ['essentielle', 'confort', 'serenite'];

interface LigneDevis {
  libelle: string;
  montantFormate: string;
}

@Component({
  selector: 'iard-devis-formule',
  imports: [DevisEntete, RouterLink],
  styleUrls: ['../devis-logement/devis-logement.scss', './devis-formule.scss'],
  templateUrl: './devis-formule.html',
})
export class DevisFormule {
  private readonly devis = inject(DevisService);
  private readonly router = inject(Router);
  private readonly sectionFranchise = viewChild<ElementRef<HTMLElement>>('sectionFranchise');

  private readonly tentative = signal(false);

  protected readonly recommandation = this.devis.recommandation;
  protected readonly choisie = this.devis.formuleChoisie;
  protected readonly optionsChoisies = this.devis.optionsChoisies;
  protected readonly franchise = this.devis.franchiseChoisie;
  protected readonly afficherAnnuel = computed(() => this.devis.periodicite() === 'annuel');

  protected readonly cartes = computed(() => {
    const tarif = this.devis.tarifBase() ?? 0;
    const mult = this.afficherAnnuel() ? 12 : 1;
    return ORDRE.map((code) => ({
      code,
      ...FORMULES[code],
      prix: formaterPrix(prixFormule(tarif, code) * mult),
      conseillee: code === this.recommandation()?.formule,
    }));
  });

  protected readonly nomConseille = computed(() => this.cartes().find((c) => c.conseillee)?.nom);

  protected readonly optionsConseillees = computed(() =>
    (this.recommandation()?.options ?? []).map((o) => LIBELLES_OPTIONS[o]),
  );

  protected readonly alerteFormuleInferieure = computed(() => {
    const choisie = this.choisie();
    const reco = this.recommandation();
    if (!choisie || !reco) return false;
    return RANG_FORMULE[choisie] < RANG_FORMULE[reco.formule];
  });

  protected readonly alerteCapitalInsuffisant = computed(() => {
    const choisie = this.choisie();
    const valeurContenu = this.devis.besoins().valeurContenu;
    if (!choisie || valeurContenu === null) return false;
    return valeurContenu > FORMULES[choisie].plafondValeur;
  });

  protected readonly prixTotalMensuel = computed(() => {
    const formule = this.choisie();
    const tarif = this.devis.tarifBase();
    if (!formule || !tarif) return null;
    const base = prixFormule(tarif, formule);
    const opts = this.optionsChoisies().reduce((s, o) => s + PRIX_OPTIONS[o], 0);
    const f = this.franchise();
    const ajustFranchise = f !== null ? AJUSTEMENTS_FRANCHISE[f] : 0;
    return Math.round((base + opts + ajustFranchise) * 100) / 100;
  });

  protected readonly lignesDevis = computed((): LigneDevis[] => {
    const formule = this.choisie();
    const tarif = this.devis.tarifBase();
    if (!formule || !tarif) return [];
    const mult = this.afficherAnnuel() ? 12 : 1;
    return [
      ...this.lignesFormule(formule, tarif, mult),
      ...this.lignesOptions(mult),
      ...this.lignesFranchise(mult),
    ];
  });

  protected readonly totalFormate = computed(() => {
    const total = this.prixTotalMensuel();
    if (total === null) return null;
    const mult = this.afficherAnnuel() ? 12 : 1;
    return formaterPrix(Math.round(total * mult * 100) / 100);
  });

  protected readonly totalMensuelFormate = computed(() => {
    const total = this.prixTotalMensuel();
    return total !== null ? formaterPrix(total) : null;
  });

  protected readonly unitePrix = computed(() => (this.afficherAnnuel() ? '/an' : '/mois'));

  protected readonly erreurFranchise = computed(() => this.tentative() && !this.franchise());

  protected readonly TOUTES_OPTIONS: readonly { code: OptionConseillee; libelle: string; prix: number }[] = [
    { code: 'objets-valeur', libelle: 'Objets de valeur', prix: PRIX_OPTIONS['objets-valeur'] },
    { code: 'assistance-depannage', libelle: 'Assistance dépannage', prix: PRIX_OPTIONS['assistance-depannage'] },
    { code: 'protection-juridique', libelle: 'Protection juridique', prix: PRIX_OPTIONS['protection-juridique'] },
  ];

  protected readonly FRANCHISES: readonly { valeur: Franchise; libelle: string; ajustement: number }[] = [
    { valeur: '150', libelle: 'Franchise 150 €', ajustement: AJUSTEMENTS_FRANCHISE['150'] },
    { valeur: '300', libelle: 'Franchise 300 € (base)', ajustement: AJUSTEMENTS_FRANCHISE['300'] },
    { valeur: '500', libelle: 'Franchise 500 €', ajustement: AJUSTEMENTS_FRANCHISE['500'] },
  ];

  protected choisir(formule: Formule): void {
    this.devis.formuleChoisie.set(formule);
  }

  protected toggleOption(option: OptionConseillee): void {
    this.devis.toggleOption(option);
  }

  protected choisirFranchise(valeur: Franchise): void {
    this.devis.franchiseChoisie.set(valeur);
  }

  protected choisirPeriodicite(p: 'mensuel' | 'annuel'): void {
    this.devis.periodicite.set(p);
  }

  protected souscrire(e: Event): void {
    e.preventDefault();
    this.tentative.set(true);
    if (!this.franchise()) {
      this.sectionFranchise()?.nativeElement.querySelector<HTMLInputElement>('input[type="radio"]')?.focus();
      return;
    }
    void this.router.navigate(['/devis/informations']);
  }

  protected formaterAjustement(montant: number): string {
    const mult = this.afficherAnnuel() ? 12 : 1;
    const signe = montant >= 0 ? '+' : '-';
    return `${signe}${formaterPrix(Math.abs(montant) * mult)}`;
  }

  private lignesFormule(formule: Formule, tarif: number, mult: number): LigneDevis[] {
    return [{ libelle: `Formule ${FORMULES[formule].nom}`, montantFormate: formaterPrix(prixFormule(tarif, formule) * mult) }];
  }

  private lignesOptions(mult: number): LigneDevis[] {
    return this.optionsChoisies().map((o) => ({
      libelle: LIBELLES_OPTIONS[o],
      montantFormate: `+${formaterPrix(PRIX_OPTIONS[o] * mult)}`,
    }));
  }

  private lignesFranchise(mult: number): LigneDevis[] {
    const f = this.franchise();
    if (!f) return [];
    const ajust = AJUSTEMENTS_FRANCHISE[f];
    const signe = ajust >= 0 ? '+' : '-';
    return [{ libelle: `Franchise ${f} €`, montantFormate: `${signe}${formaterPrix(Math.abs(ajust) * mult)}` }];
  }
}
