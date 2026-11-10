import { Component, DestroyRef, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { emailValide } from '../../core/model/compte';
import { ResultatConnexion } from '../../core/model/connexion';
import { ConnexionService } from '../../core/service/connexion.service';
import { MotDePasseService } from '../../core/service/mot-de-passe.service';

/** US-07 : longueur maximale d'une adresse e-mail, espaces autour non comptés. */
const LONGUEUR_MAX_EMAIL = 254;

function adressePourReinitialisation(email: string): boolean {
  return emailValide(email) && email.trim().length <= LONGUEUR_MAX_EMAIL;
}

@Component({
  selector: 'iard-connexion',
  imports: [RouterLink],
  styleUrl: '../inscription/inscription.scss',
  templateUrl: './connexion.html',
})
export class Connexion {
  private readonly connexionService = inject(ConnexionService);
  private readonly motDePasseService = inject(MotDePasseService);
  private readonly router = inject(Router);
  private readonly retour = inject(ActivatedRoute).snapshot.queryParamMap.get('retour');
  protected readonly parametresRetour = this.retour ? { retour: this.retour } : {};
  private readonly champEmail = viewChild.required<ElementRef<HTMLInputElement>>('champEmail');
  private readonly champMotDePasse = viewChild.required<ElementRef<HTMLInputElement>>('champMotDePasse');
  private finBlocage?: ReturnType<typeof setTimeout>;

  protected readonly email = signal('');
  protected readonly motDePasse = signal('');
  private readonly tentative = signal(false);

  protected readonly erreurEmail = computed(() => this.tentative() && !emailValide(this.email()));
  protected readonly erreurMotDePasse = computed(() => this.tentative() && !this.motDePasse());
  protected readonly identifiantsIncorrects = signal(false);
  protected readonly erreurTechnique = signal(false);
  protected readonly envoiEnCours = signal(false);
  protected readonly adresseRequise = signal(false);
  protected readonly lienEnvoye = signal(false);
  /** Heure de fin du blocage, déjà mise en forme ; null si le compte n'est pas bloqué. */
  protected readonly bloqueJusquA = signal<string | null>(null);

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.finBlocage));
  }

  protected saisirEmail(event: Event): void {
    this.email.set((event.target as HTMLInputElement).value);
    this.identifiantsIncorrects.set(false);
    this.adresseRequise.set(false);
    this.lienEnvoye.set(false);
    this.leverBlocage();
  }

  protected saisirMotDePasse(event: Event): void {
    this.motDePasse.set((event.target as HTMLInputElement).value);
    this.identifiantsIncorrects.set(false);
  }

  protected seConnecter(event: Event): void {
    event.preventDefault();
    if (this.bloqueJusquA()) {
      return;
    }
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
    this.identifiantsIncorrects.set(false);
    this.erreurTechnique.set(false);
    this.connexionService.connecter(this.email(), this.motDePasse()).subscribe((resultat) => {
      this.envoiEnCours.set(false);
      this.traiter(resultat);
    });
  }

  /** AC1, AC2 et RG1 de l'US-07 : possible même compte bloqué, le message ne dit pas si le compte existe. */
  protected motDePasseOublie(): void {
    this.lienEnvoye.set(false);
    this.identifiantsIncorrects.set(false);
    this.erreurTechnique.set(false);
    if (!adressePourReinitialisation(this.email())) {
      this.adresseRequise.set(true);
      this.champEmail().nativeElement.focus();
      return;
    }
    this.adresseRequise.set(false);
    this.envoiEnCours.set(true);
    this.motDePasseService.demanderLien(this.email()).subscribe((envoye) => {
      this.envoiEnCours.set(false);
      this.lienEnvoye.set(envoye);
      this.erreurTechnique.set(!envoye);
    });
  }

  private traiter(resultat: ResultatConnexion): void {
    switch (resultat.statut) {
      case 'code-envoye':
        void this.router.navigate(['/double-authentification'], {
          queryParams: this.parametresRetour,
        });
        break;
      case 'identifiants-incorrects':
        this.identifiantsIncorrects.set(true);
        break;
      case 'compte-bloque':
        this.bloquer(new Date(resultat.bloqueJusquA));
        break;
      default:
        this.erreurTechnique.set(true);
    }
  }

  private bloquer(fin: Date): void {
    this.bloqueJusquA.set(fin.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }));
    clearTimeout(this.finBlocage);
    this.finBlocage = setTimeout(() => this.bloqueJusquA.set(null), fin.getTime() - Date.now());
  }

  /** Le blocage porte sur un compte : changer d'adresse permet de réessayer, le backend reste seul juge. */
  private leverBlocage(): void {
    clearTimeout(this.finBlocage);
    this.bloqueJusquA.set(null);
  }
}
