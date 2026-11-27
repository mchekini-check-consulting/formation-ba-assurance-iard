import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

type SousEtape = '1.1' | '1.2' | '1.3' | '1.4' | '2' | '3' | '4' | '5' | '6';

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
    ['1.2', '1.3', '1.4', '2'].includes(this.sousEtape()),
  );
  protected readonly etape12Terminee = computed(() =>
    ['1.3', '1.4', '2'].includes(this.sousEtape()),
  );
  protected readonly etape13Terminee = computed(() =>
    this.sousEtape() === '1.4' || this.sousEtape() === '2',
  );

  protected readonly etape1Active = computed(() =>
    ['1.1', '1.2', '1.3', '1.4'].includes(this.sousEtape()),
  );
  protected readonly etape1Faite = computed(() => ['2', '3', '4', '5', '6'].includes(this.sousEtape()));
  protected readonly etape2Active = computed(() => this.sousEtape() === '2');
  protected readonly etape2Faite = computed(() => ['3', '4', '5', '6'].includes(this.sousEtape()));
  protected readonly etape3Active = computed(() => this.sousEtape() === '3');
  protected readonly etape3Faite = computed(() => ['4', '5', '6'].includes(this.sousEtape()));
  protected readonly etape4Active = computed(() => this.sousEtape() === '4');
  protected readonly etape4Faite = computed(() => ['5', '6'].includes(this.sousEtape()));
  protected readonly etape5Active = computed(() => this.sousEtape() === '5');
  protected readonly etape5Faite = computed(() => this.sousEtape() === '6');
  protected readonly etape6Active = computed(() => this.sousEtape() === '6');
  protected readonly montrerSousEtapes = computed(() => !['2', '3', '4', '5', '6'].includes(this.sousEtape()));
}
