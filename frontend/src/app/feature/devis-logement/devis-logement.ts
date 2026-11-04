import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  Antecedent,
  StatutLogement,
  TypeLogement,
  TypeResidence,
  codePostalValide,
  colocatairesValides,
  estimerTarif,
  surfaceValide,
} from '../../core/model/devis';
import { DevisService } from '../../core/service/devis.service';

@Component({
  selector: 'iard-devis-logement',
  imports: [RouterLink],
  styleUrl: './devis-logement.scss',
  templateUrl: './devis-logement.html',
})
export class DevisLogement {
  private readonly devisService = inject(DevisService);
  private readonly router = inject(Router);

  private readonly premierBoutonStatut = viewChild<ElementRef<HTMLButtonElement>>('premierBoutonStatut');
  private readonly premierBoutonColocation = viewChild<ElementRef<HTMLButtonElement>>('premierBoutonColocation');
  private readonly premierBoutonDependances = viewChild<ElementRef<HTMLButtonElement>>('premierBoutonDependances');
  private readonly champResidence = viewChild<ElementRef<HTMLSelectElement>>('champResidence');
  private readonly champType = viewChild<ElementRef<HTMLSelectElement>>('champType');
  private readonly champNombrePiecesSelect = viewChild<ElementRef<HTMLSelectElement>>('champNombrePiecesSelect');
  private readonly champSurface = viewChild<ElementRef<HTMLInputElement>>('champSurface');
  private readonly champAdresse = viewChild<ElementRef<HTMLInputElement>>('champAdresse');
  private readonly champCodePostal = viewChild<ElementRef<HTMLInputElement>>('champCodePostal');
  private readonly champVille = viewChild<ElementRef<HTMLInputElement>>('champVille');
  private readonly champEtage = viewChild<ElementRef<HTMLInputElement>>('champEtage');
  private readonly champColocataires = viewChild<ElementRef<HTMLInputElement>>('champColocataires');
  private readonly champSinistresSelect = viewChild<ElementRef<HTMLSelectElement>>('champSinistresSelect');
  private readonly champResilientContrat = viewChild<ElementRef<HTMLSelectElement>>('champResilientContrat');
  private readonly champAssuranceAntecedent = viewChild<ElementRef<HTMLSelectElement>>('champAssuranceAntecedent');
  private readonly champAssureurActuel = viewChild<ElementRef<HTMLInputElement>>('champAssureurActuel');

  private readonly tentative = signal(false);
  private readonly groupeLocataire = signal(false);

  protected readonly statut = signal<StatutLogement | null>(null);
  protected readonly residence = signal<TypeResidence | null>(null);
  protected readonly typeLogement = signal<TypeLogement | null>(null);
  protected readonly adresse = signal('');
  protected readonly codePostal = signal('');
  protected readonly ville = signal('');
  protected readonly surface = signal('');
  protected readonly nombrePieces = signal('');
  protected readonly etage = signal('');
  protected readonly dependances = signal<boolean | null>(null);
  protected readonly nombreColocataires = signal('');
  protected readonly nombreSinistres = signal('');
  protected readonly resilieContrat = signal('');
  protected readonly assuranceAntecedent = signal('');
  protected readonly assureurActuel = signal('');

  // antecedent dérivé des deux selects de la carte antécédents
  protected readonly antecedent = computed<Antecedent | null>(() => {
    const r = this.resilieContrat();
    if (r === 'non-paiement') return 'resiliation-non-paiement';
    if (r === 'autre') return 'resiliation-autre';
    if (r === 'non') {
      const a = this.assuranceAntecedent();
      if (a === 'non-assure') return 'non-assure';
      if (a === 'assure-moins-un-an') return 'assure-moins-un-an';
      if (a === 'assure-plus-un-an') return 'assure-plus-un-an';
    }
    return null;
  });

  // AC3
  protected readonly estNonOccupant = computed(() => this.statut() === 'proprietaire-non-occupant');
  // AC2
  protected readonly estAppartement = computed(() => this.typeLogement() === 'appartement');
  protected readonly estMaison = computed(() => this.typeLogement() === 'maison');
  protected readonly estColocation = computed(() => this.statut() === 'colocation');
  protected readonly estLocataireOuColocation = computed(
    () => this.groupeLocataire() || this.statut() === 'locataire' || this.statut() === 'colocation',
  );
  protected readonly estAssurePlusUnAn = computed(() => this.antecedent() === 'assure-plus-un-an');
  // AC4
  protected readonly estBloque = computed(() => this.antecedent() === 'resiliation-non-paiement');
  // AC6
  protected readonly tarifEstime = computed(() => estimerTarif(this.surface(), this.typeLogement()));
  protected readonly tarifFormate = computed(() => {
    const t = this.tarifEstime();
    return t !== null ? t.toFixed(2).replace('.', ',') : null;
  });

  // AC5 — erreurs de validation
  protected readonly erreurStatut = computed(() => this.tentative() && !this.statut() && !this.groupeLocataire());
  protected readonly erreurSousStatut = computed(() => this.tentative() && this.groupeLocataire() && !this.statut());
  protected readonly erreurResidence = computed(
    () => this.tentative() && !this.estNonOccupant() && !this.residence(),
  );
  protected readonly erreurTypeLogement = computed(
    () => this.tentative() && !this.estNonOccupant() && !this.typeLogement(),
  );
  protected readonly erreurNombrePieces = computed(
    () => this.tentative() && !this.estNonOccupant() && !this.nombrePieces().trim(),
  );
  protected readonly erreurSurface = computed(
    () => this.tentative() && !this.estNonOccupant() && !surfaceValide(this.surface()),
  );
  protected readonly erreurAdresse = computed(
    () => this.tentative() && !this.estNonOccupant() && !this.adresse().trim(),
  );
  protected readonly erreurCodePostal = computed(
    () => this.tentative() && !this.estNonOccupant() && !codePostalValide(this.codePostal()),
  );
  protected readonly erreurVille = computed(
    () => this.tentative() && !this.estNonOccupant() && !this.ville().trim(),
  );
  protected readonly erreurEtage = computed(
    () => this.tentative() && this.estAppartement() && !this.etage().trim(),
  );
  protected readonly erreurDependances = computed(
    () => this.tentative() && this.estMaison() && this.dependances() === null,
  );
  protected readonly erreurNombreColocataires = computed(
    () => this.tentative() && this.estColocation() && !colocatairesValides(this.nombreColocataires()),
  );
  protected readonly erreurNombreSinistres = computed(
    () => this.tentative() && !this.estNonOccupant() && !this.nombreSinistres(),
  );
  protected readonly erreurResilientContrat = computed(
    () => this.tentative() && !this.estNonOccupant() && !this.resilieContrat(),
  );
  protected readonly erreurAssuranceAntecedent = computed(
    () => this.tentative() && !this.estNonOccupant() && !this.assuranceAntecedent(),
  );
  protected readonly erreurAntecedent = computed(
    () => this.erreurResilientContrat() || this.erreurAssuranceAntecedent(),
  );
  protected readonly erreurAssureurActuel = computed(
    () => this.tentative() && this.estAssurePlusUnAn() && !this.assureurActuel().trim(),
  );

  private readonly formulaireValide = computed(
    () =>
      !this.erreurStatut() &&
      !this.erreurSousStatut() &&
      !this.erreurResidence() &&
      !this.erreurTypeLogement() &&
      !this.erreurNombrePieces() &&
      !this.erreurSurface() &&
      !this.erreurAdresse() &&
      !this.erreurCodePostal() &&
      !this.erreurVille() &&
      !this.erreurEtage() &&
      !this.erreurDependances() &&
      !this.erreurNombreColocataires() &&
      !this.erreurNombreSinistres() &&
      !this.erreurAntecedent() &&
      !this.erreurAssureurActuel(),
  );

  protected choisirGroupeLocataire(): void {
    this.groupeLocataire.set(true);
    this.statut.set(null);
  }

  protected choisirStatut(s: StatutLogement): void {
    if (s !== 'locataire' && s !== 'colocation') this.groupeLocataire.set(false);
    this.statut.set(s);
  }

  protected choisirResidenceDepuisSelect(e: Event): void {
    const val = (e.target as HTMLSelectElement).value as TypeResidence | '';
    this.residence.set(val || null);
  }

  protected choisirTypeDepuisSelect(e: Event): void {
    const val = (e.target as HTMLSelectElement).value as TypeLogement | '';
    this.typeLogement.set(val || null);
  }

  protected choisirNombrePiecesDepuisSelect(e: Event): void {
    this.nombrePieces.set((e.target as HTMLSelectElement).value);
  }

  protected saisirSurface(e: Event): void {
    const input = e.target as HTMLInputElement;
    const val = input.value.replace(/\D/g, '').slice(0, 4);
    input.value = val;
    this.surface.set(val);
  }

  protected saisirAdresse(e: Event): void {
    this.adresse.set((e.target as HTMLInputElement).value);
  }

  protected saisirCodePostal(e: Event): void {
    const input = e.target as HTMLInputElement;
    const val = input.value.replace(/\D/g, '').slice(0, 5);
    input.value = val;
    this.codePostal.set(val);
  }

  protected saisirVille(e: Event): void {
    this.ville.set((e.target as HTMLInputElement).value);
  }

  protected saisirEtage(e: Event): void {
    this.etage.set((e.target as HTMLInputElement).value);
  }

  protected choisirDependances(v: boolean): void {
    this.dependances.set(v);
  }

  protected saisirNombreColocataires(e: Event): void {
    const input = e.target as HTMLInputElement;
    const val = input.value.replace(/\D/g, '').slice(0, 2);
    input.value = val;
    this.nombreColocataires.set(val);
  }

  protected choisirSinistresDepuisSelect(e: Event): void {
    this.nombreSinistres.set((e.target as HTMLSelectElement).value);
  }

  protected choisirResilientContratDepuisSelect(e: Event): void {
    this.resilieContrat.set((e.target as HTMLSelectElement).value);
  }

  protected choisirAssuranceAntecedentDepuisSelect(e: Event): void {
    this.assuranceAntecedent.set((e.target as HTMLSelectElement).value);
  }

  protected saisirAssureurActuel(e: Event): void {
    this.assureurActuel.set((e.target as HTMLInputElement).value);
  }

  protected continuer(e: Event): void {
    e.preventDefault();
    this.tentative.set(true);
    if (this.estNonOccupant()) return;
    if (!this.formulaireValide()) {
      this.focusPremierChampErreur();
      return;
    }
    this.devisService.mettreAJour({
      statut: this.statut(),
      residence: this.residence(),
      typeLogement: this.typeLogement(),
      adresse: this.adresse(),
      codePostal: this.codePostal(),
      ville: this.ville(),
      surface: this.surface(),
      nombrePieces: this.nombrePieces(),
      antecedent: this.antecedent(),
      etage: this.etage(),
      dependances: this.dependances(),
      nombreColocataires: this.nombreColocataires(),
      assureurActuel: this.assureurActuel(),
      nombreSinistres: this.nombreSinistres(),
    });
    void this.router.navigate(['/devis/besoins']);
  }

  // PROVISOIRE : redirige vers /contact en attendant un formulaire de rappel CRM
  protected rappelerConseiller(): void {
    void this.router.navigate(['/contact']);
  }

  private focusPremierChampErreur(): void {
    if (this.erreurStatut()) { this.premierBoutonStatut()?.nativeElement.focus(); return; }
    if (this.erreurSousStatut()) { this.premierBoutonColocation()?.nativeElement.focus(); return; }
    if (this.erreurResidence()) { this.champResidence()?.nativeElement.focus(); return; }
    if (this.erreurTypeLogement()) { this.champType()?.nativeElement.focus(); return; }
    if (this.erreurNombrePieces()) { this.champNombrePiecesSelect()?.nativeElement.focus(); return; }
    if (this.erreurSurface()) { this.champSurface()?.nativeElement.focus(); return; }
    if (this.erreurAdresse()) { this.champAdresse()?.nativeElement.focus(); return; }
    if (this.erreurCodePostal()) { this.champCodePostal()?.nativeElement.focus(); return; }
    if (this.erreurVille()) { this.champVille()?.nativeElement.focus(); return; }
    if (this.erreurEtage()) { this.champEtage()?.nativeElement.focus(); return; }
    if (this.erreurDependances()) { this.premierBoutonDependances()?.nativeElement.focus(); return; }
    if (this.erreurNombreColocataires()) { this.champColocataires()?.nativeElement.focus(); return; }
    if (this.erreurNombreSinistres()) { this.champSinistresSelect()?.nativeElement.focus(); return; }
    if (this.erreurResilientContrat()) { this.champResilientContrat()?.nativeElement.focus(); return; }
    if (this.erreurAssuranceAntecedent()) { this.champAssuranceAntecedent()?.nativeElement.focus(); return; }
    if (this.erreurAssureurActuel()) { this.champAssureurActuel()?.nativeElement.focus(); return; }
  }
}
