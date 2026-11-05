import { Injectable, computed, signal } from '@angular/core';
import { DevisLogement, StatutLogement, TypeLogement, estimerTarif, logementVide } from '../model/devis';
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

  readonly recommandation = computed(() => recommander(this.besoins()));
  readonly tarifBase = computed(() => {
    const { surface, typeLogement } = this.logement();
    return estimerTarif(surface, typeLogement);
  });

  /** Cotisation par mois : formule, options et franchise choisies. */
  readonly prixMensuel = computed(() => {
    const formule = this.formuleChoisie();
    const tarif = this.tarifBase();
    if (!formule || !tarif) return null;
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
