import { Injectable, computed, signal } from '@angular/core';
import { DevisLogement, StatutLogement, TypeLogement, estimerTarif, logementVide } from '../model/devis';
import { DemandeDevis, RepriseDevis } from '../model/devis-enregistre';
import { SaisiePaiement, paiementVide } from '../model/devis-paiement';
import { SaisieInformations, saisieVide } from '../model/informations';
import {
  AJUSTEMENTS_FRANCHISE,
  Formule,
  Franchise,
  OptionConseillee,
  PRIX_OPTIONS,
  Periodicite,
  ReponsesBesoins,
  prixFormule,
  recommander,
  reponsesVides,
} from '../model/recommandation';

const CLE_BROUILLON = 'assureasy.devis';

interface PrixGaranti {
  prix: number;
  choix: string;
}

/** Copie du devis le temps d'une connexion ou d'une création de compte. */
interface Brouillon {
  logement: DevisLogement;
  besoins: ReponsesBesoins;
  reference: string | null;
  formule: Formule | null;
  options: OptionConseillee[];
  franchise: Franchise | null;
  periodicite: Periodicite;
}

@Injectable({ providedIn: 'root' })
export class DevisService {
  readonly logement = signal<DevisLogement>(logementVide());
  readonly besoins = signal<ReponsesBesoins>(reponsesVides());
  /** Référence de la recommandation enregistrée : vide dès qu'une réponse change. */
  readonly reference = signal<string | null>(null);
  readonly formuleChoisie = signal<Formule | null>(null);
  readonly optionsChoisies = signal<OptionConseillee[]>([]);
  readonly franchiseChoisie = signal<Franchise | null>(null);
  readonly periodicite = signal<Periodicite>('mensuel');
  /** Étape 4, fichiers compris : gardée en mémoire jusqu'à un rechargement de la page. */
  readonly informations = signal<SaisieInformations>(saisieVide());
  /** Devis pour lequel l'étape 4 a été enregistrée : l'étape 5 n'est ouverte que pour celui-là. */
  readonly referenceDossier = signal<string | null>(null);
  readonly paiement = signal<SaisiePaiement>(paiementVide());
  /** Vrai une fois le mandat SEPA signé avec succès à l'étape 5. */
  readonly mandatSigne = signal<boolean>(false);
  /** US-12 : prix garanti d'un devis repris, valable pour les mêmes choix. */
  private readonly prixGaranti = signal<PrixGaranti | null>(null);
  /** US-12 : message à afficher sur l'écran de formule après une reprise. */
  readonly messageReprise = signal<string | null>(null);

  readonly recommandation = computed(() => recommander(this.besoins()));
  readonly tarifBase = computed(() => {
    const { surface, typeLogement } = this.logement();
    return estimerTarif(surface, typeLogement);
  });

  private readonly choixTarifes = computed(() => {
    const { surface, typeLogement } = this.logement();
    const options = [...this.optionsChoisies()].sort((a, b) => a.localeCompare(b)).join(',');
    return [surface, typeLogement, this.formuleChoisie(), options, this.franchiseChoisie()].join('|');
  });

  /** US-12 : ce qu'il faut envoyer pour enregistrer ou télécharger le devis ; null s'il est incomplet. */
  readonly demandeDevis = computed((): DemandeDevis | null => {
    const reference = this.reference();
    const formule = this.formuleChoisie();
    const franchise = this.franchiseChoisie();
    if (!reference || !formule || !franchise) return null;
    return { reference, logement: this.logement(), formule, options: this.optionsChoisies(), franchise };
  });

  /** Cotisation par mois : formule, options et franchise choisies. */
  readonly prixMensuel = computed(() => {
    const formule = this.formuleChoisie();
    const tarif = this.tarifBase();
    if (!formule || !tarif) return null;
    const garanti = this.prixGaranti();
    if (garanti?.choix === this.choixTarifes()) return garanti.prix;
    const base = prixFormule(tarif, formule);
    const options = this.optionsChoisies().reduce((somme, option) => somme + PRIX_OPTIONS[option], 0);
    const franchise = this.franchiseChoisie();
    const ajustement = franchise !== null ? AJUSTEMENTS_FRANCHISE[franchise] : 0;
    return Math.round((base + options + ajustement) * 100) / 100;
  });

  mettreAJour(partiel: Partial<DevisLogement>): void {
    this.logement.update((actuel) => ({ ...actuel, ...partiel }));
  }

  mettreAJourBesoins(partiel: Partial<ReponsesBesoins>): void {
    this.besoins.update((actuel) => ({ ...actuel, ...partiel }));
    this.reference.set(null);
  }

  // AC3 : la formule conseillée est sélectionnée à l'arrivée sur l'écran de choix
  validerRecommandation(reference: string, formule: Formule): void {
    this.reference.set(reference);
    this.formuleChoisie.set(formule);
  }

  toggleOption(option: OptionConseillee): void {
    this.optionsChoisies.update((opts) =>
      opts.includes(option) ? opts.filter((o) => o !== option) : [...opts, option],
    );
  }

  /** US-12, AC5 : remet le devis repris en place. Le mandat d'un autre devis ne doit pas lui être rattaché. */
  restaurer(reprise: RepriseDevis): void {
    this.logement.set(reprise.logement);
    this.besoins.set(reprise.besoins);
    this.reference.set(reprise.reference);
    this.formuleChoisie.set(reprise.formule);
    this.optionsChoisies.set(reprise.options);
    this.franchiseChoisie.set(reprise.franchise);
    this.periodicite.set('mensuel');
    this.referenceDossier.set(null);
    this.paiement.set(paiementVide());
    this.mandatSigne.set(false);
    this.prixGaranti.set({ prix: reprise.prixMensuel, choix: this.choixTarifes() });
  }

  /** US-12, AC2 : protège les réponses d'un rechargement pendant la connexion ; effacé à la fermeture de l'onglet. */
  conserverBrouillon(): void {
    const brouillon: Brouillon = {
      logement: this.logement(),
      besoins: this.besoins(),
      reference: this.reference(),
      formule: this.formuleChoisie(),
      options: this.optionsChoisies(),
      franchise: this.franchiseChoisie(),
      periodicite: this.periodicite(),
    };
    try {
      sessionStorage.setItem(CLE_BROUILLON, JSON.stringify(brouillon));
    } catch {
      // Sans stockage disponible, les réponses restent en mémoire.
    }
  }

  restaurerBrouillon(): void {
    const brouillon = this.lireBrouillon();
    if (!brouillon) return;
    this.logement.set(brouillon.logement);
    this.besoins.set(brouillon.besoins);
    this.reference.set(brouillon.reference);
    this.formuleChoisie.set(brouillon.formule);
    this.optionsChoisies.set(brouillon.options);
    this.franchiseChoisie.set(brouillon.franchise);
    this.periodicite.set(brouillon.periodicite);
  }

  oublierBrouillon(): void {
    try {
      sessionStorage.removeItem(CLE_BROUILLON);
    } catch {
      // Rien à effacer si le stockage n'est pas disponible.
    }
  }

  private lireBrouillon(): Brouillon | null {
    try {
      const contenu = sessionStorage.getItem(CLE_BROUILLON);
      return contenu ? (JSON.parse(contenu) as Brouillon) : null;
    } catch {
      return null;
    }
  }

  // PROVISOIRE : 'proprietaire' de la landing est mappé sur 'proprietaire-occupant' par défaut
  prefillDepuisLanding(
    statut: 'locataire' | 'proprietaire',
    typeLogement: TypeLogement,
    codePostal: string,
  ): void {
    const statutDevis: StatutLogement = statut === 'locataire' ? 'locataire' : 'proprietaire-occupant';
    this.mettreAJour({ statut: statutDevis, typeLogement, codePostal });
  }
}
