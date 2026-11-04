import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FORMULES, Formule, LIBELLES_OPTIONS, formaterPrix, prixFormule } from '../../core/model/recommandation';
import { DevisService } from '../../core/service/devis.service';
import { DevisEntete } from '../devis-entete/devis-entete';

const ORDRE: Formule[] = ['essentielle', 'confort', 'serenite'];

@Component({
  selector: 'iard-devis-formule',
  imports: [DevisEntete, RouterLink],
  styleUrls: ['../devis-logement/devis-logement.scss', './devis-formule.scss'],
  templateUrl: './devis-formule.html',
})
export class DevisFormule {
  private readonly devis = inject(DevisService);

  protected readonly recommandation = this.devis.recommandation;
  protected readonly choisie = this.devis.formuleChoisie;

  protected readonly cartes = computed(() => {
    const tarif = this.devis.tarifBase() ?? 0;
    return ORDRE.map((code) => ({
      code,
      ...FORMULES[code],
      prix: formaterPrix(prixFormule(tarif, code)),
      conseillee: code === this.recommandation()?.formule,
    }));
  });
  protected readonly nomConseille = computed(() => this.cartes().find((carte) => carte.conseillee)?.nom);
  protected readonly options = computed(() =>
    (this.recommandation()?.options ?? []).map((option) => LIBELLES_OPTIONS[option]),
  );

  protected choisir(formule: Formule): void {
    this.devis.formuleChoisie.set(formule);
  }
}
