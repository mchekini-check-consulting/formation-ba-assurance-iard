import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MotifNouveauCode, ResultatCode, codeValide, pageDeRetour } from '../../core/model/connexion';
import { ConnexionService } from '../../core/service/connexion.service';

@Component({
  selector: 'iard-double-authentification',
  imports: [RouterLink],
  styleUrl: '../inscription/inscription.scss',
  templateUrl: './double-authentification.html',
})
export class DoubleAuthentification {
  private readonly connexionService = inject(ConnexionService);
  private readonly router = inject(Router);
  private readonly retour = inject(ActivatedRoute).snapshot.queryParamMap.get('retour');
  private readonly champCode = viewChild.required<ElementRef<HTMLInputElement>>('champCode');

  protected readonly emailMasque = computed(() => this.connexionService.verification()?.emailMasque);
  protected readonly code = signal('');
  private readonly tentative = signal(false);
  protected readonly erreurCode = computed(() => this.tentative() && !codeValide(this.code()));
  protected readonly essaisRestants = signal<number | null>(null);
  protected readonly nouveauCodeRequis = signal<MotifNouveauCode | null>(null);
  protected readonly erreurTechnique = signal(false);
  protected readonly envoiEnCours = signal(false);
  protected readonly annonce = signal<string | null>(null);

  protected saisirCode(event: Event): void {
    const champ = event.target as HTMLInputElement;
    const chiffres = champ.value.replace(/\D/g, '').slice(0, 6);
    champ.value = chiffres;
    this.code.set(chiffres);
  }

  protected valider(event: Event): void {
    event.preventDefault();
    if (this.nouveauCodeRequis()) {
      return;
    }
    this.tentative.set(true);
    if (this.erreurCode()) {
      this.champCode().nativeElement.focus();
      return;
    }
    this.commencerEnvoi();
    this.connexionService.verifierCode(this.code()).subscribe((resultat) => {
      this.envoiEnCours.set(false);
      this.traiter(resultat);
    });
  }

  protected renvoyerCode(): void {
    this.commencerEnvoi();
    this.connexionService.renvoyerCode().subscribe((resultat) => {
      this.envoiEnCours.set(false);
      if (resultat === 'verification-perdue') {
        void this.router.navigate(['/connexion']);
      } else if (resultat === 'erreur-technique') {
        this.erreurTechnique.set(true);
      } else {
        this.repartirDeZero();
      }
    });
  }

  protected fermerAnnonce(): void {
    this.annonce.set(null);
  }

  private commencerEnvoi(): void {
    this.envoiEnCours.set(true);
    this.erreurTechnique.set(false);
    this.annonce.set(null);
  }

  private traiter(resultat: ResultatCode): void {
    switch (resultat.statut) {
      case 'connecte':
        void this.router.navigateByUrl(pageDeRetour(this.retour));
        break;
      case 'code-incorrect':
        this.essaisRestants.set(resultat.essaisRestants);
        this.champCode().nativeElement.focus();
        break;
      case 'nouveau-code-requis':
        this.essaisRestants.set(null);
        this.nouveauCodeRequis.set(resultat.motif);
        break;
      case 'verification-perdue':
        void this.router.navigate(['/connexion']);
        break;
      default:
        this.erreurTechnique.set(true);
    }
  }

  private repartirDeZero(): void {
    this.nouveauCodeRequis.set(null);
    this.essaisRestants.set(null);
    this.tentative.set(false);
    this.code.set('');
    this.champCode().nativeElement.value = '';
    this.champCode().nativeElement.focus();
    this.annonce.set('Un nouveau code vient de vous être envoyé.');
  }
}
