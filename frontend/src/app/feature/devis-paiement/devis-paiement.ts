import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SaisiePaiement } from '../../core/model/devis-paiement';
import { erreurIban, formaterIban, ibanMasque, normaliserIban } from '../../core/model/iban';
import { dateVersIso, formaterSaisieDate, nombreAutresColocataires } from '../../core/model/informations';
import {
  JOURS_PRELEVEMENT,
  JourPrelevement,
  dateDepuisSaisie,
  erreurDateDebut,
  formaterDate,
  premierPrelevement,
} from '../../core/model/premier-prelevement';
import { blocsRecapitulatif } from '../../core/model/recapitulatif';
import { formaterPrix } from '../../core/model/recommandation';
import { DevisService } from '../../core/service/devis.service';
import { MandatPrepare, MandatService, ResultatSignature } from '../../core/service/mandat.service';
import { DevisEntete } from '../devis-entete/devis-entete';

const A_RENSEIGNER = 'À renseigner';
const ID_DU_CHAMP: Readonly<Record<string, string>> = {
  dateDebut: 'date-debut',
  iban: 'iban',
  jourPrelevement: 'jour-prelevement',
};

@Component({
  selector: 'iard-devis-paiement',
  imports: [DevisEntete, RouterLink],
  styleUrls: [
    '../devis-logement/devis-logement.scss',
    '../devis-besoins/devis-besoins.scss',
    '../devis-informations/devis-informations.scss',
    './devis-paiement.scss',
  ],
  templateUrl: './devis-paiement.html',
})
export class DevisPaiement {
  private readonly devis = inject(DevisService);
  private readonly mandats = inject(MandatService);
  private readonly router = inject(Router);
  private readonly page = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly aujourdhui = new Date();

  private readonly tentative = signal(false);
  private readonly quittes = signal<ReadonlySet<string>>(new Set());
  private readonly refus = signal<Readonly<Record<string, string>>>({});

  protected readonly jours = JOURS_PRELEVEMENT;
  protected readonly saisie = this.devis.paiement;
  // RG5 : jamais conservée, la case est toujours décochée à l'arrivée sur l'écran
  protected readonly autorisation = signal(false);
  protected readonly mandat = signal<MandatPrepare | null>(null);
  protected readonly erreurPreparation = signal(false);
  protected readonly erreurEnvoi = signal(false);
  protected readonly envoi = signal(false);

  private readonly dateDebut = computed(() =>
    erreurDateDebut(this.saisie().dateDebut, this.aujourdhui) ? null : dateDepuisSaisie(this.saisie().dateDebut),
  );

  // RG4
  private readonly datePremierPrelevement = computed(() => {
    const debut = this.dateDebut();
    const jour = this.saisie().jour;
    return debut && jour ? premierPrelevement(debut, jour, this.aujourdhui) : null;
  });

  /** Dans l'ordre de l'écran : la première erreur reçoit le curseur (AC4). */
  private readonly controles = computed(() => {
    const s = this.saisie();
    return new Map<string, string | null>([
      ['date-debut', erreurDateDebut(s.dateDebut, this.aujourdhui)],
      ['titulaire', s.titulaire.trim() ? null : 'Indiquez le titulaire du compte.'],
      ['iban', erreurIban(s.iban)],
      ['jour-prelevement', s.jour ? null : 'Choisissez votre jour de prélèvement.'],
    ]);
  });

  protected readonly blocs = computed(() => {
    const { statut, nombreColocataires } = this.devis.logement();
    const nombre = statut === 'colocation' ? nombreAutresColocataires(nombreColocataires) : 0;
    return blocsRecapitulatif({
      logement: this.devis.logement(),
      besoins: this.devis.besoins(),
      formule: this.devis.formuleChoisie(),
      options: this.devis.optionsChoisies(),
      franchise: this.devis.franchiseChoisie(),
      prixMensuel: this.devis.prixMensuel(),
      informations: this.devis.informations(),
      colocataires: this.devis.informations().colocataires.slice(0, nombre),
    });
  });

  protected readonly lignesPaiement = computed(() => {
    const s = this.saisie();
    const prix = this.devis.prixMensuel();
    const premier = this.datePremierPrelevement();
    const ibanRenseigne = !erreurIban(s.iban);
    return [
      { libelle: 'Date de début', valeur: this.dateDebut() ? s.dateDebut : A_RENSEIGNER },
      { libelle: 'Titulaire du compte', valeur: s.titulaire.trim() || A_RENSEIGNER },
      { libelle: 'IBAN', valeur: ibanRenseigne ? ibanMasque(s.iban) : A_RENSEIGNER },
      { libelle: 'Jour de prélèvement', valeur: s.jour ? `Le ${s.jour} de chaque mois` : A_RENSEIGNER },
      { libelle: 'Premier prélèvement', valeur: premier ? formaterDate(premier) : A_RENSEIGNER },
      { libelle: 'Montant', valeur: prix === null ? '' : `${formaterPrix(prix)} €/mois` },
    ];
  });

  constructor() {
    this.preRemplirTitulaire();
    this.preparerMandat();
  }

  protected erreur(id: string): string | null {
    const refus = this.refus()[id];
    if (refus) return refus;
    return this.tentative() || this.quittes().has(id) ? (this.controles().get(id) ?? null) : null;
  }

  protected quitterChamp(id: string): void {
    this.quittes.update((quittes) => new Set(quittes).add(id));
  }

  protected saisirDate(e: Event): void {
    const champ = e.target as HTMLInputElement;
    champ.value = formaterSaisieDate(champ.value);
    this.mettreAJour({ dateDebut: champ.value }, 'date-debut');
  }

  protected saisirTitulaire(e: Event): void {
    this.mettreAJour({ titulaire: (e.target as HTMLInputElement).value }, 'titulaire');
  }

  protected saisirIban(e: Event): void {
    const champ = e.target as HTMLInputElement;
    champ.value = formaterIban(champ.value);
    this.mettreAJour({ iban: champ.value }, 'iban');
  }

  protected choisirJour(jour: JourPrelevement): void {
    this.mettreAJour({ jour }, 'jour-prelevement');
  }

  protected basculerAutorisation(e: Event): void {
    this.autorisation.set((e.target as HTMLInputElement).checked);
  }

  protected modifierPaiement(): void {
    this.placerCurseur('date-debut');
  }

  protected generer(e: Event): void {
    e.preventDefault();
    if (!this.autorisation() || !this.mandat() || this.envoi()) return;
    this.tentative.set(true);
    this.erreurEnvoi.set(false);
    const premiereErreur = [...this.controles()].find(([, message]) => message)?.[0];
    if (premiereErreur) {
      this.placerCurseur(premiereErreur);
      return;
    }
    this.envoyer();
  }

  private preRemplirTitulaire(): void {
    const { prenom, nom } = this.devis.informations();
    this.saisie.update((s) => (s.titulaire ? s : { ...s, titulaire: `${prenom.trim()} ${nom.trim()}`.trim() }));
  }

  private preparerMandat(): void {
    this.mandats.preparer(this.devis.reference() as string).subscribe((resultat) => {
      if (resultat.statut === 'prepare') {
        this.mandat.set(resultat.mandat);
      } else if (resultat.statut === 'session-expiree') {
        this.reconnecter();
      } else {
        this.erreurPreparation.set(true);
      }
    });
  }

  private mettreAJour(partiel: Partial<SaisiePaiement>, id: string): void {
    this.saisie.update((s) => ({ ...s, ...partiel }));
    this.refus.update((refus) => ({ ...refus, [id]: '' }));
  }

  private placerCurseur(id: string): void {
    this.page.querySelector<HTMLElement>(`#${id}`)?.focus();
  }

  private reconnecter(): void {
    void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis/paiement' } });
  }

  private envoyer(): void {
    const s = this.saisie();
    this.envoi.set(true);
    this.mandats
      .signer({
        referenceDevis: this.devis.reference() as string,
        titulaire: s.titulaire,
        iban: normaliserIban(s.iban),
        dateDebut: dateVersIso(s.dateDebut),
        jourPrelevement: s.jour as JourPrelevement,
        autorisation: this.autorisation(),
      })
      .subscribe((resultat) => {
        this.envoi.set(false);
        this.traiterResultat(resultat);
      });
  }

  private traiterResultat(resultat: ResultatSignature): void {
    if (resultat.statut === 'signe') {
      void this.router.navigate(['/devis/contrat']);
    } else if (resultat.statut === 'session-expiree') {
      this.reconnecter();
    } else if (resultat.statut === 'refuse' && ID_DU_CHAMP[resultat.champ]) {
      const id = ID_DU_CHAMP[resultat.champ];
      this.refus.update((refus) => ({ ...refus, [id]: resultat.message }));
      this.placerCurseur(id);
    } else {
      this.erreurEnvoi.set(true);
    }
  }
}
