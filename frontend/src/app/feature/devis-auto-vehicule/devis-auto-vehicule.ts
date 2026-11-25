import { Component, ElementRef, OnInit, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MarqueVehicule, ModeleVehicule, SivRechercheResponse, VehiculeAuto } from '../../core/model/devis-auto';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { SivService } from '../../core/service/siv.service';
import { MARQUES_AUTRES } from './data/marques';
import { DevisAutoLayout } from '../devis-auto-layout/devis-auto-layout';

export type ModeDevisAuto = 'plaque' | 'marque';

function modeValide(valeur: string | null | undefined): ModeDevisAuto | null {
  return valeur === 'plaque' || valeur === 'marque' ? valeur : null;
}

function normaliserChaine(s: string): string {
  return s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

@Component({
  selector: 'iard-devis-auto-vehicule',
  imports: [RouterLink, DevisAutoLayout],
  templateUrl: './devis-auto-vehicule.html',
  styleUrl: './devis-auto-vehicule.scss',
})
export class DevisAutoVehicule implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sivService = inject(SivService);
  protected readonly devisAutoService = inject(DevisAutoService);

  protected readonly modeActuel = signal<ModeDevisAuto>('plaque');
  protected readonly afficherBlocPlaque = computed(() => this.modeActuel() !== 'marque');

  protected readonly immatriculation = signal('');
  protected readonly tentative = signal(false);
  protected readonly envoi = signal(false);
  protected readonly resultatSiv = signal<SivRechercheResponse | null>(null);
  protected readonly erreurReseau = signal(false);
  protected readonly marques = signal<MarqueVehicule[]>([]);
  protected readonly chargementReferentiel = signal(false);

  protected readonly marqueSelectionnee = signal<string | null>(null);
  protected readonly autreMarqueActif = signal(false);
  protected readonly rechercheMarque = signal('');
  protected readonly indexSuggestion = signal(-1);
  protected readonly modeleNomSelectionne = signal<string | null>(null);
  protected readonly energieSelectionnee = signal<string | null>(null);
  protected readonly versionSelectionnee = signal<string | null>(null);
  protected readonly anneeSelectionnee = signal<number | null>(null);
  protected readonly nonImmatricule = signal(false);
  protected readonly MARQUES_VEDETTES = ['Renault', 'Peugeot', 'Citroën', 'Toyota', 'Volkswagen', 'Dacia', 'Ford'];

  private readonly champPlaque = viewChild<ElementRef<HTMLInputElement>>('champPlaque');
  private readonly champRecherche = viewChild<ElementRef<HTMLInputElement>>('champRecherche');

  protected readonly modelesDeMarque = computed(() => {
    const m = this.marqueSelectionnee();
    return m ? (this.marques().find(x => x.marque === m)?.modeles ?? []) : [];
  });

  protected readonly nomModeles = computed(() =>
    [...new Set(this.modelesDeMarque().map(m => m.modele))]
  );

  protected readonly energiesDisponibles = computed(() => {
    const n = this.modeleNomSelectionne();
    return n
      ? [...new Set(this.modelesDeMarque().filter(m => m.modele === n).map(m => m.energie))]
      : [];
  });

  protected readonly versionsDisponibles = computed(() => {
    const n = this.modeleNomSelectionne(), e = this.energieSelectionnee();
    return (n && e)
      ? [...new Set(this.modelesDeMarque().filter(m => m.modele === n && m.energie === e).map(m => m.version ?? ''))]
      : [];
  });

  protected readonly anneesDisponibles = computed(() => {
    const n = this.modeleNomSelectionne(), e = this.energieSelectionnee(), v = this.versionSelectionnee();
    if (!n || !e) return [];
    return [...new Set(
      this.modelesDeMarque()
        .filter(m => m.modele === n && m.energie === e && (!v || m.version === v))
        .map(m => m.annee)
        .filter((a): a is number => a !== null)
    )];
  });

  protected readonly suggestionsVisibles = computed(() => {
    const q = this.rechercheMarque();
    if (q.length < 2) return [];
    const qn = normaliserChaine(q);
    return MARQUES_AUTRES.filter(m => normaliserChaine(m).includes(qn));
  });

  protected readonly modeleVehiculeChoisi = computed((): ModeleVehicule | null => {
    const n = this.modeleNomSelectionne(), e = this.energieSelectionnee();
    if (!n || !e) return null;
    const v = this.versionSelectionnee(), a = this.anneeSelectionnee();
    return this.modelesDeMarque().find(
      m => m.modele === n && m.energie === e && (!v || m.version === v) && (!a || m.annee === a)
    ) ?? null;
  });

  constructor() {
    const stateMode = (history.state as { mode?: string })?.mode;
    const queryMode = this.route.snapshot.queryParamMap.get('mode');
    this.modeActuel.set(modeValide(stateMode) ?? modeValide(queryMode) ?? 'plaque');

    // Réagit aux changements d'URL (bouton Précédent/Suivant du navigateur)
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.modeActuel.set(modeValide(params.get('mode')) ?? 'plaque');
    });

    afterNextRender(() => {
      if (this.afficherBlocPlaque()) {
        this.champPlaque()?.nativeElement.focus();
      }
    });
  }

  ngOnInit(): void {
    this.chargementReferentiel.set(true);
    this.sivService.referentielVehicules().subscribe((marques) => {
      this.marques.set(marques);
      this.chargementReferentiel.set(false);
    });
  }

  protected basculerVers(mode: ModeDevisAuto): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { mode },
    });
  }

  protected saisirPlaque(e: Event): void {
    this.immatriculation.set((e.target as HTMLInputElement).value.toUpperCase());
  }

  protected chercherParPlaque(e: Event): void {
    e.preventDefault();
    this.tentative.set(true);
    if (!this.immatriculation().trim() || this.envoi()) return;
    this.envoi.set(true);
    this.resultatSiv.set(null);
    this.erreurReseau.set(false);
    this.sivService.chercherParPlaque(this.immatriculation().trim()).subscribe((reponse) => {
      this.envoi.set(false);
      if (reponse === null) {
        this.erreurReseau.set(true);
      } else {
        this.resultatSiv.set(reponse);
      }
    });
  }

  protected selectionnerMarqueTile(marque: string): void {
    this.autreMarqueActif.set(false);
    this.marqueSelectionnee.set(marque);
    this.modeleNomSelectionne.set(null);
    this.energieSelectionnee.set(null);
    this.versionSelectionnee.set(null);
    this.anneeSelectionnee.set(null);
    this.nonImmatricule.set(false);
  }

  protected activerAutreMarque(): void {
    this.autreMarqueActif.set(true);
    this.marqueSelectionnee.set(null);
    this.rechercheMarque.set('');
    this.indexSuggestion.set(-1);
    this.modeleNomSelectionne.set(null);
    this.energieSelectionnee.set(null);
    this.versionSelectionnee.set(null);
    this.anneeSelectionnee.set(null);
    this.nonImmatricule.set(false);
    setTimeout(() => this.champRecherche()?.nativeElement.focus(), 0);
  }

  protected saisirRecherche(e: Event): void {
    this.rechercheMarque.set((e.target as HTMLInputElement).value);
    this.indexSuggestion.set(-1);
  }

  protected choisirSuggestion(marque: string): void {
    this.rechercheMarque.set(marque);
    this.indexSuggestion.set(-1);
    this.marqueSelectionnee.set(marque);
    this.modeleNomSelectionne.set(null);
    this.energieSelectionnee.set(null);
    this.versionSelectionnee.set(null);
    this.anneeSelectionnee.set(null);
    this.nonImmatricule.set(false);
  }

  protected naviguerSuggestions(e: KeyboardEvent): void {
    const suggestions = this.suggestionsVisibles();
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      this.indexSuggestion.update(i => Math.min(i + 1, suggestions.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      this.indexSuggestion.update(i => Math.max(i - 1, -1));
    } else if (e.key === 'Enter' && this.indexSuggestion() >= 0) {
      e.preventDefault();
      this.choisirSuggestion(suggestions[this.indexSuggestion()]);
    } else if (e.key === 'Escape') {
      this.rechercheMarque.set('');
      this.indexSuggestion.set(-1);
    }
  }

  protected choisirModeleNomPill(nom: string): void {
    this.modeleNomSelectionne.set(nom);
    this.energieSelectionnee.set(null);
    this.versionSelectionnee.set(null);
    this.anneeSelectionnee.set(null);
  }

  protected choisirEnergiePill(energie: string): void {
    this.energieSelectionnee.set(energie);
    this.versionSelectionnee.set(null);
    this.anneeSelectionnee.set(null);
  }

  protected choisirVersionPill(version: string): void {
    this.versionSelectionnee.set(version);
    this.anneeSelectionnee.set(null);
  }

  protected choisirModeleNom(e: Event): void {
    this.modeleNomSelectionne.set((e.target as HTMLSelectElement).value || null);
    this.energieSelectionnee.set(null);
    this.versionSelectionnee.set(null);
    this.anneeSelectionnee.set(null);
  }

  protected choisirEnergie(e: Event): void {
    this.energieSelectionnee.set((e.target as HTMLSelectElement).value || null);
    this.versionSelectionnee.set(null);
    this.anneeSelectionnee.set(null);
  }

  protected choisirVersion(e: Event): void {
    this.versionSelectionnee.set((e.target as HTMLSelectElement).value || null);
    this.anneeSelectionnee.set(null);
  }

  protected choisirAnnee(e: Event): void {
    const v = Number((e.target as HTMLSelectElement).value);
    this.anneeSelectionnee.set(v || null);
  }

  protected confirmerVehiculeSiv(siv: SivRechercheResponse): void {
    const vehicule: VehiculeAuto = {
      refVehiculeId: siv.refVehiculeId!,
      immatriculation: siv.immatriculation,
      marque: siv.marque!,
      modele: siv.modele!,
      energie: siv.energie!,
      version: siv.version ?? null,
      annee: siv.annee ?? null,
      valeurNeuve: siv.valeurNeuve ?? null,
    };
    this.devisAutoService.enregistrerVehicule(vehicule);
    void this.router.navigate(['/devis-auto/achat']);
  }

  protected confirmerVehiculeReferentiel(): void {
    const marque = this.marqueSelectionnee();
    const modele = this.modeleVehiculeChoisi();
    if (!marque || !modele) return;
    this.devisAutoService.enregistrerVehicule({
      refVehiculeId: modele.id,
      immatriculation: null,
      marque,
      modele: modele.modele,
      energie: modele.energie,
      version: modele.version,
      annee: modele.annee,
      valeurNeuve: modele.valeurNeuve,
    });
    void this.router.navigate(['/devis-auto/achat']);
  }
}
