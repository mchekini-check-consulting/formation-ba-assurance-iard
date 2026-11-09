import { Component, DestroyRef, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, Subject, debounceTime, of, switchMap } from 'rxjs';
import { MENTION_CONSERVATION, formaterDate } from '../../core/model/devis-enregistre';
import {
  FORMULES,
  Formule,
  Franchise,
  LIBELLES_OPTIONS,
  OptionConseillee,
  RANG_FORMULE,
  formaterPrix,
} from '../../core/model/recommandation';
import { Tarif, recommandationDe } from '../../core/model/tarif';
import { ConnexionService } from '../../core/service/connexion.service';
import { DevisEnregistreService } from '../../core/service/devis-enregistre.service';
import { DevisService } from '../../core/service/devis.service';
import { TarifService } from '../../core/service/tarif.service';
import { DevisEntete } from '../devis-entete/devis-entete';

const ORDRE: Formule[] = ['essentielle', 'confort', 'serenite'];

const RETOUR_ENREGISTREMENT = { retour: '/devis/formule?enregistrer=1' };
const NOM_PDF = 'devis-assureasy.pdf';
const ATTENTE_RECALCUL_MS = 300;
const ERREUR_TARIF = "Votre tarif n'a pas pu être calculé. Réessayez dans quelques instants.";

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
export class DevisFormule implements OnInit {
  private readonly devis = inject(DevisService);
  private readonly router = inject(Router);
  private readonly devisEnregistres = inject(DevisEnregistreService);
  private readonly connexion = inject(ConnexionService);
  private readonly route = inject(ActivatedRoute);
  private readonly tarifService = inject(TarifService);
  private readonly destruction = inject(DestroyRef);
  private readonly choixModifies = new Subject<void>();
  private readonly sectionFranchise = viewChild<ElementRef<HTMLElement>>('sectionFranchise');

  private readonly tentative = signal(false);

  protected readonly proposerCompte = signal(false);
  protected readonly envoiEnCours = signal(false);
  protected readonly telechargementEnCours = signal(false);
  protected readonly finValidite = signal<string | null>(null);
  protected readonly erreurDevis = signal<string | null>(null);
  protected readonly calculEnCours = signal(false);
  protected readonly erreurTarif = signal<string | null>(null);
  protected readonly RETOUR_ENREGISTREMENT = RETOUR_ENREGISTREMENT;
  protected readonly MENTION_CONSERVATION = MENTION_CONSERVATION;
  protected readonly messageReprise = this.devis.messageReprise;

  protected readonly recommandation = this.devis.recommandation;
  protected readonly choisie = this.devis.formuleChoisie;
  protected readonly optionsChoisies = this.devis.optionsChoisies;
  protected readonly franchise = this.devis.franchiseChoisie;
  protected readonly afficherAnnuel = computed(() => this.devis.periodicite() === 'annuel');

  protected readonly cartes = computed(() => {
    const prix = this.devis.tarif()?.prix ?? [];
    return ORDRE.map((code) => {
      const ligne = prix.find((p) => p.formule === code);
      const montant = this.afficherAnnuel() ? ligne?.annuel : ligne?.mensuel;
      return {
        code,
        ...FORMULES[code],
        prix: montant === undefined ? '' : formaterPrix(montant),
        conseillee: code === this.recommandation()?.formule,
      };
    });
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

  protected readonly prixTotalMensuel = this.devis.prixMensuel;

  protected readonly lignesDevis = computed((): LigneDevis[] => {
    const detail = this.devis.tarif()?.detail;
    if (!detail) return [];
    const mult = this.afficherAnnuel() ? 12 : 1;
    const lignes: LigneDevis[] = [
      { libelle: `Formule ${FORMULES[detail.formule].nom}`, montantFormate: formaterPrix(detail.prixFormule * mult) },
      ...detail.options.map((o) => ({
        libelle: LIBELLES_OPTIONS[o.option],
        montantFormate: `+${formaterPrix(o.prixMensuel * mult)}`,
      })),
    ];
    const franchise = this.franchise();
    if (franchise) {
      lignes.push({
        libelle: `Franchise ${franchise} €`,
        montantFormate: this.formaterAjustement(detail.ajustementFranchise),
      });
    }
    return lignes;
  });

  protected readonly totalFormate = computed(() => {
    const detail = this.devis.tarif()?.detail;
    if (!detail) return null;
    return formaterPrix(this.afficherAnnuel() ? detail.totalAnnuel : detail.totalMensuel);
  });

  protected readonly totalMensuelFormate = computed(() => {
    const total = this.prixTotalMensuel();
    return total !== null ? formaterPrix(total) : null;
  });

  protected readonly unitePrix = computed(() => (this.afficherAnnuel() ? '/an' : '/mois'));

  protected readonly erreurFranchise = computed(() => this.tentative() && !this.franchise());

  protected readonly toutesOptions = computed(() =>
    (this.devis.tarif()?.prixOptions ?? []).map((o) => ({
      code: o.option,
      libelle: LIBELLES_OPTIONS[o.option],
      prix: o.prixMensuel,
    })),
  );

  protected readonly franchises = computed(() =>
    (this.devis.tarif()?.ajustementsFranchise ?? []).map((f) => ({
      valeur: String(f.franchise) as Franchise,
      libelle: `Franchise ${f.franchise} €${f.ajustement === 0 ? ' (base)' : ''}`,
      ajustement: f.ajustement,
    })),
  );

  constructor() {
    this.choixModifies
      .pipe(
        debounceTime(ATTENTE_RECALCUL_MS),
        switchMap(() => this.calculer()),
        takeUntilDestroyed(this.destruction),
      )
      .subscribe((tarif) => this.recevoirTarif(tarif));
  }

  ngOnInit(): void {
    this.chargerTarif();
    // AC2 : de retour de la connexion, le devis s'enregistre sans nouveau clic
    if (this.route.snapshot.queryParamMap.get('enregistrer')) {
      this.envoyerDevis();
      void this.router.navigate(['/devis/formule'], { replaceUrl: true });
    }
  }

  protected choisir(formule: Formule): void {
    this.devis.formuleChoisie.set(formule);
    this.finValidite.set(null);
    this.recalculer();
  }

  protected toggleOption(option: OptionConseillee): void {
    this.devis.toggleOption(option);
    this.finValidite.set(null);
    this.recalculer();
  }

  protected choisirFranchise(valeur: Franchise): void {
    this.devis.franchiseChoisie.set(valeur);
    this.finValidite.set(null);
    this.recalculer();
  }

  protected choisirPeriodicite(p: 'mensuel' | 'annuel'): void {
    this.devis.periodicite.set(p);
  }

  protected souscrire(e: Event): void {
    e.preventDefault();
    this.tentative.set(true);
    if (this.franchiseManquante() || this.calculEnCours() || this.prixTotalMensuel() === null) return;
    void this.router.navigate(['/devis/informations']);
  }

  /** RG2 : l'enregistrement exige un compte. */
  protected enregistrerDevis(): void {
    if (this.envoiEnCours() || this.calculEnCours() || this.franchiseManquante()) return;
    this.erreurDevis.set(null);
    this.envoiEnCours.set(true);
    this.connexion.sessionValide().subscribe((valide) => {
      this.envoiEnCours.set(false);
      if (valide) {
        this.envoyerDevis();
      } else {
        this.proposerUnCompte();
      }
    });
  }

  /** RG2 et RG3 : sans compte, généré par le backend, sans envoi par e-mail. */
  protected telechargerDevis(): void {
    const demande = this.devis.demandeDevis();
    if (this.telechargementEnCours() || this.calculEnCours() || this.franchiseManquante() || !demande) return;
    this.erreurDevis.set(null);
    this.telechargementEnCours.set(true);
    this.devisEnregistres.telechargerPdf(demande).subscribe((fichier) => {
      this.telechargementEnCours.set(false);
      if (fichier) {
        this.enregistrerFichier(fichier);
      } else {
        this.erreurDevis.set("Le PDF n'a pas pu être généré. Réessayez dans quelques instants.");
      }
    });
  }

  private envoyerDevis(): void {
    const demande = this.devis.demandeDevis();
    if (!demande) return;
    this.envoiEnCours.set(true);
    this.devisEnregistres.enregistrer(demande).subscribe((resultat) => {
      this.envoiEnCours.set(false);
      if (resultat.statut === 'enregistre') {
        this.finValidite.set(formaterDate(resultat.dateFinValidite));
        this.proposerCompte.set(false);
        this.devis.oublierBrouillon();
      } else if (resultat.statut === 'session-expiree') {
        this.proposerUnCompte();
      } else {
        this.erreurDevis.set("Votre devis n'a pas pu être enregistré. Réessayez dans quelques instants.");
      }
    });
  }

  private proposerUnCompte(): void {
    this.devis.conserverBrouillon();
    this.proposerCompte.set(true);
  }

  private franchiseManquante(): boolean {
    this.tentative.set(true);
    if (this.franchise()) return false;
    this.sectionFranchise()?.nativeElement.querySelector<HTMLInputElement>('input[type="radio"]')?.focus();
    return true;
  }

  private enregistrerFichier(fichier: Blob): void {
    const adresse = URL.createObjectURL(fichier);
    const lien = document.createElement('a');
    lien.href = adresse;
    lien.download = NOM_PDF;
    lien.click();
    URL.revokeObjectURL(adresse);
  }

  protected formaterAjustement(montant: number): string {
    const mult = this.afficherAnnuel() ? 12 : 1;
    const signe = montant >= 0 ? '+' : '-';
    return `${signe}${formaterPrix(Math.abs(montant) * mult)}`;
  }

  private chargerTarif(): void {
    const tarif = this.devis.tarif();
    const reference = this.devis.reference();
    if (tarif?.detail || !reference) return;
    this.calculEnCours.set(true);
    const origine: Observable<Tarif | null> = tarif ? of(null) : this.tarifService.tarifRetenu(reference);
    origine
      .pipe(
        switchMap((retrouve) => (retrouve ? of(retrouve) : this.calculer())),
        takeUntilDestroyed(this.destruction),
      )
      .subscribe((resultat) => this.recevoirTarif(resultat));
  }

  private recalculer(): void {
    this.calculEnCours.set(true);
    this.erreurTarif.set(null);
    this.choixModifies.next();
  }

  private calculer(): Observable<Tarif | null> {
    return this.tarifService.tarifer(this.devis.reference() ?? '', {
      logement: this.devis.logement(),
      formule: this.choisie(),
      options: this.optionsChoisies(),
      franchise: this.franchise(),
    });
  }

  private recevoirTarif(tarif: Tarif | null): void {
    this.calculEnCours.set(false);
    if (tarif === null) {
      this.devis.tarif.update((actuel) => actuel && { ...actuel, detail: undefined });
      this.erreurTarif.set(ERREUR_TARIF);
      return;
    }
    if (!tarif.tarifable) {
      this.erreurTarif.set(tarif.motif ?? ERREUR_TARIF);
      return;
    }
    this.devis.tarif.set(tarif);
    const recommandation = recommandationDe(tarif);
    if (recommandation) this.devis.recommandation.set(recommandation);
  }
}
