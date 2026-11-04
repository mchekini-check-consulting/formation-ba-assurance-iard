import { Injectable, computed, signal } from '@angular/core';
import { DevisLogement, StatutLogement, TypeLogement, estimerTarif, logementVide } from '../model/devis';
import { Formule, ReponsesBesoins, recommander, reponsesVides } from '../model/recommandation';

@Injectable({ providedIn: 'root' })
export class DevisService {
  readonly logement = signal<DevisLogement>(logementVide());
  readonly besoins = signal<ReponsesBesoins>(reponsesVides());
  /** Référence de la recommandation enregistrée : vide dès qu'une réponse change. */
  readonly reference = signal<string | null>(null);
  readonly formuleChoisie = signal<Formule | null>(null);

  readonly recommandation = computed(() => recommander(this.besoins()));
  readonly tarifBase = computed(() => {
    const { surface, typeLogement } = this.logement();
    return estimerTarif(surface, typeLogement);
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
