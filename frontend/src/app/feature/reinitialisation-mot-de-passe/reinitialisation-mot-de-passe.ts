import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { motDePasseConforme, reglesMotDePasse } from '../../core/model/compte';
import { MotDePasseService, ResultatLien } from '../../core/service/mot-de-passe.service';

type Etape = 'verification' | 'saisie' | 'lien-invalide' | 'reinitialise' | 'indisponible';

@Component({
  selector: 'iard-reinitialisation-mot-de-passe',
  imports: [RouterLink],
  styleUrl: '../inscription/inscription.scss',
  templateUrl: './reinitialisation-mot-de-passe.html',
})
export class ReinitialisationMotDePasse {
  private readonly motDePasseService = inject(MotDePasseService);
  private readonly jeton = inject(ActivatedRoute).snapshot.queryParamMap.get('jeton');
  private readonly champMotDePasse = viewChild<ElementRef<HTMLInputElement>>('champMotDePasse');

  protected readonly etape = signal<Etape>('verification');
  protected readonly motDePasse = signal('');
  private readonly tentative = signal(false);
  protected readonly regles = computed(() => reglesMotDePasse(this.motDePasse()));
  protected readonly erreurMotDePasse = computed(() => this.tentative() && !motDePasseConforme(this.motDePasse()));
  protected readonly erreurTechnique = signal(false);
  protected readonly envoiEnCours = signal(false);

  constructor() {
    if (!this.jeton) {
      this.etape.set('lien-invalide');
      return;
    }
    this.motDePasseService.verifierLien(this.jeton).subscribe((resultat) => {
      if (resultat === 'erreur-technique') {
        this.etape.set('indisponible');
      } else {
        this.etape.set(resultat === 'valide' ? 'saisie' : 'lien-invalide');
      }
    });
  }

  protected saisirMotDePasse(event: Event): void {
    this.motDePasse.set((event.target as HTMLInputElement).value);
  }

  protected valider(event: Event): void {
    event.preventDefault();
    this.tentative.set(true);
    if (this.erreurMotDePasse()) {
      this.champMotDePasse()?.nativeElement.focus();
      return;
    }
    this.envoiEnCours.set(true);
    this.erreurTechnique.set(false);
    this.motDePasseService.reinitialiser(this.jeton as string, this.motDePasse()).subscribe((resultat) => {
      this.envoiEnCours.set(false);
      this.traiter(resultat);
    });
  }

  private traiter(resultat: ResultatLien): void {
    if (resultat === 'erreur-technique') {
      this.erreurTechnique.set(true);
    } else {
      this.etape.set(resultat === 'valide' ? 'reinitialise' : 'lien-invalide');
    }
  }
}
