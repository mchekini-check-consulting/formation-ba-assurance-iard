import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

type SousEtape = '1.1' | '1.2' | '1.3' | '1.4';

@Component({
  selector: 'iard-devis-auto-layout',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './devis-auto-layout.html',
  styleUrl: './devis-auto-layout.scss',
})
export class DevisAutoLayout {
  readonly sousEtape = input.required<SousEtape>();

  protected readonly etape11Terminee = computed(() =>
    ['1.2', '1.3', '1.4'].includes(this.sousEtape()),
  );
  protected readonly etape12Terminee = computed(() =>
    ['1.3', '1.4'].includes(this.sousEtape()),
  );
  protected readonly etape13Terminee = computed(() => this.sousEtape() === '1.4');
}
