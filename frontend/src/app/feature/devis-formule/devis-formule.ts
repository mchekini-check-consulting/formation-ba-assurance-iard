import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MENTION_CONSERVATION, formaterDate } from '../../core/model/devis-enregistre';
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
import { ConnexionService } from '../../core/service/connexion.service';
import { DevisEnregistreService } from '../../core/service/devis-enregistre.service';
import { DevisService } from '../../core/service/devis.service';
import { DevisEntete } from '../devis-entete/devis-entete';

const ORDRE: Formule[] = ['essentielle', 'confort', 'serenite'];

const RETOUR_ENREGISTREMENT = { retour: '/devis/formule?enregistrer=1' };
const NOM_PDF = 'devis-assureasy.pdf';

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
  private readonly sectionFranchise = viewChild<ElementRef<HTMLElement>>('sectionFranchise');

  private readonly tentative = signal(false);

  protected readonly proposerCompte = signal(false);
  protected readonly envoiEnCours = signal(false);
  protected readonly telechargementEnCours = signal(false);
  protected readonly finValidite = signal<string | null>(null);
  protected readonly erreurDevis = signal<string | null>(null);
  protected readonly RETOUR_ENREGISTREMENT = RETOUR_ENREGISTREMENT;
  protected readonly MENTION_CONSERVATION = MENTION_CONSERVATION;
  protected readonly messageReprise = this.devis.messageReprise;

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

  protected readonly prixTotalMensuel = this.devis.prixMensuel;

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

  ngOnInit(): void {
    // AC2 : de retour de la connexion, le devis s'enregistre sans nouveau clic
    if (this.route.snapshot.queryParamMap.get('enregistrer')) {
      this.envoyerDevis();
      void this.router.navigate(['/devis/formule'], { replaceUrl: true });
    }
  }

  protected choisir(formule: Formule): void {
    this.devis.formuleChoisie.set(formule);
    this.finValidite.set(null);
  }

  protected toggleOption(option: OptionConseillee): void {
    this.devis.toggleOption(option);
    this.finValidite.set(null);
  }

  protected choisirFranchise(valeur: Franchise): void {
    this.devis.franchiseChoisie.set(valeur);
    this.finValidite.set(null);
  }

  protected choisirPeriodicite(p: 'mensuel' | 'annuel'): void {
    this.devis.periodicite.set(p);
  }

  protected souscrire(e: Event): void {
    e.preventDefault();
    this.tentative.set(true);
    if (this.franchiseManquante()) return;
    void this.router.navigate(['/devis/informations']);
  }

  /** RG2 : l'enregistrement exige un compte. */
  protected enregistrerDevis(): void {
    if (this.envoiEnCours() || this.franchiseManquante()) return;
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
    if (this.telechargementEnCours() || this.franchiseManquante() || !demande) return;
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
