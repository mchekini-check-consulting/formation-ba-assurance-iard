import { Component, computed, input } from '@angular/core';
import { decouperPlaques } from '../../core/model/texte';

/**
 * US-21 (recette) : affiche un texte sans jamais couper une immatriculation sur deux lignes (la plaque est insécable,
 * le reste du texte revient à la ligne normalement). Le texte lu et copié est inchangé.
 */
@Component({
  selector: 'iard-texte-plaque',
  // Sur une seule ligne : aucun espace ajouté entre les morceaux.
  template: `@for (morceau of morceaux(); track $index) {@if (morceau.plaque) {<span class="plaque">{{ morceau.texte }}</span>} @else {<ng-container>{{ morceau.texte }}</ng-container>}}`,
  styles: `.plaque { white-space: nowrap; }`,
})
export class TextePlaque {
  readonly texte = input<string | null | undefined>('');
  protected readonly morceaux = computed(() => decouperPlaques(this.texte()));
}
