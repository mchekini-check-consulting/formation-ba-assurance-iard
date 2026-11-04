import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { emailValide, motDePasseConforme, reglesMotDePasse } from '../../core/model/compte';
import { CompteService } from '../../core/service/compte.service';

@Component({
  selector: 'iard-inscription',
  imports: [RouterLink],
  styleUrl: './inscription.scss',
  templateUrl: './inscription.html',
})
export class Inscription {
  private readonly compteService = inject(CompteService);
  private readonly router = inject(Router);
  private readonly champEmail = viewChild.required<ElementRef<HTMLInputElement>>('champEmail');
  private readonly champMotDePasse = viewChild.required<ElementRef<HTMLInputElement>>('champMotDePasse');

  protected readonly email = signal('');
  protected readonly motDePasse = signal('');
  private readonly tentative = signal(false);

  protected readonly regles = computed(() => reglesMotDePasse(this.motDePasse()));
  protected readonly erreurEmail = computed(() => this.tentative() && !emailValide(this.email()));
  protected readonly erreurMotDePasse = computed(() => this.tentative() && !motDePasseConforme(this.motDePasse()));
  protected readonly emailDejaUtilise = signal(false);
  protected readonly erreurTechnique = signal(false);
  protected readonly envoiEnCours = signal(false);

  protected saisirEmail(event: Event): void {
    this.email.set((event.target as HTMLInputElement).value);
    this.emailDejaUtilise.set(false);
  }

  protected saisirMotDePasse(event: Event): void {
    this.motDePasse.set((event.target as HTMLInputElement).value);
  }

  protected creerCompte(event: Event): void {
    event.preventDefault();
    this.tentative.set(true);
    if (this.erreurEmail()) {
      this.champEmail().nativeElement.focus();
      return;
    }
    if (this.erreurMotDePasse()) {
      this.champMotDePasse().nativeElement.focus();
      return;
    }
    this.envoiEnCours.set(true);
    this.erreurTechnique.set(false);
    this.compteService.creerCompte({ email: this.email(), motDePasse: this.motDePasse() }).subscribe((resultat) => {
      this.envoiEnCours.set(false);
      if (resultat === 'erreur-technique') {
        this.erreurTechnique.set(true);
        return;
      }
      if (resultat === 'email-deja-utilise') {
        this.emailDejaUtilise.set(true);
        this.champEmail().nativeElement.focus();
        return;
      }
      void this.router.navigate(['/compte-cree']);
    });
  }
}
