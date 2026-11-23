// TODO US-27 : remplacer ce composant provisoire par la vraie étape 1.1 du devis auto.
// Lire this.mode pour orienter l'expérience :
//   'plaque'  → focus automatique dans le champ numéro de plaque
//   'marque'  → défilement jusqu'à la section « Ou par marque » + focus sur la première marque
//   null      → comportement normal (pas de mode transmis)
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

export type ModeDevisAuto = 'plaque' | 'marque';

function modeValide(valeur: string | null | undefined): ModeDevisAuto | null {
  return valeur === 'plaque' || valeur === 'marque' ? valeur : null;
}

@Component({
  selector: 'iard-devis-auto-vehicule',
  imports: [RouterLink],
  template: `
    <main style="padding: 4rem 1.5rem; max-width: 800px; margin: 0 auto; font-family: inherit;">
      <h1 style="font-size: 1.75rem; margin-bottom: 1rem;">Étape 1.1 – Retrouver mon véhicule (à venir, US-27)</h1>
      <p style="color: #4a4a4f; margin-bottom: 0.5rem;">
        Mode reçu :
        @if (mode) {
          <strong>{{ mode }}</strong>
        } @else {
          <em>(aucun mode reçu)</em>
        }
      </p>
      <a routerLink="/" style="display: inline-block; margin-top: 2rem;">← Retour à l'accueil</a>
    </main>
  `,
})
export class DevisAutoVehicule {
  private readonly route = inject(ActivatedRoute);

  protected readonly mode: ModeDevisAuto | null;

  constructor() {
    const stateMode = (history.state as { mode?: string })?.mode;
    const queryMode = this.route.snapshot.queryParamMap.get('mode');
    this.mode = modeValide(stateMode) ?? modeValide(queryMode);
  }
}
