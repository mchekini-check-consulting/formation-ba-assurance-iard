import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AnnonceService } from '../../core/service/annonce.service';
import { DevisService } from '../../core/service/devis.service';

export const TITRE_PRINCIPAL = "Votre logement et votre voiture assurés en ligne, sans conseiller ni paperasse.";
export const TEXTE_URGENCE = "Une urgence ? Incendie ou personne blessée : appelez d'abord le 112. Dépannage ou remorquage : notre assistance répond 24h/24.";
export const NUMERO_ASSISTANCE = "+33 7 59 33 19 62";
export const FORME_SOCIALE = "[FORME SOCIALE]";
export const CAPITAL_SOCIAL = "[CAPITAL]";
export const VILLE_RCS = "[VILLE]";
export const NUMERO_RCS = "[NUMÉRO]";

export type Statut = 'locataire' | 'proprietaire';
export type TypeLogement = 'appartement' | 'maison';
export type ModeAuto = 'plaque' | 'marque';

interface Question {
  question: string;
  reponse: string;
}

interface Formule {
  nom: string;
  description: string;
  plafond: string;
  miseEnAvant: boolean;
  picto: string;
}

interface FormuleAuto {
  nom: string;
  description: string;
  miseEnAvant: boolean;
  picto: string;
}

interface Etape {
  titre: string;
  detail: string;
}

export function codePostalValide(codePostal: string): boolean {
  return /^\d{5}$/.test(codePostal);
}

@Component({
  selector: 'iard-landing',
  imports: [RouterLink],
  styleUrl: './landing.scss',
  templateUrl: './landing.html',
})
export class Landing {
  private readonly devisService = inject(DevisService);
  private readonly router = inject(Router);
  private readonly champCodePostal = viewChild.required<ElementRef<HTMLInputElement>>('champCodePostal');
  private readonly btnPlaque = viewChild.required<ElementRef<HTMLButtonElement>>('btnPlaque');
  private readonly btnMarque = viewChild.required<ElementRef<HTMLButtonElement>>('btnMarque');

  protected readonly titrePrincipal = TITRE_PRINCIPAL;
  protected readonly texteUrgence = TEXTE_URGENCE;
  protected readonly numeroAssistance = NUMERO_ASSISTANCE;
  protected readonly formeSociale = FORME_SOCIALE;
  protected readonly capitalSocial = CAPITAL_SOCIAL;
  protected readonly villeRcs = VILLE_RCS;
  protected readonly numeroRcs = NUMERO_RCS;
  protected readonly ongletActif = signal<'habitation' | 'auto'>('habitation');

  protected readonly statut = signal<Statut | null>(null);
  protected readonly typeLogement = signal<TypeLogement | null>(null);
  protected readonly codePostal = signal('');
  private readonly tentative = signal(false);

  protected readonly erreurChoix = computed(() => this.tentative() && (!this.statut() || !this.typeLogement()));
  protected readonly erreurCodePostal = computed(() => this.tentative() && !codePostalValide(this.codePostal()));

  protected readonly modeAuto = signal<ModeAuto | null>(null);
  private readonly tentativeAuto = signal(false);
  protected readonly erreurModeAuto = computed(() => this.tentativeAuto() && !this.modeAuto());

  protected readonly annonce = signal<string | null>(inject(AnnonceService).prendre());


  protected readonly questionOuverte = signal(-1);

  protected readonly questions: Question[] = [
    {
      question: 'Dois-je créer un compte pour obtenir un tarif ?',
      reponse:
        "Non, le tarif s'affiche immédiatement sans compte. Un compte vous sera demandé uniquement pour souscrire.",
    },
    {
      question: 'Pouvez-vous résilier mon ancien contrat ?',
      reponse:
        "Oui. Si votre contrat a plus d'un an, Assur'Easy s'occupe de la résiliation auprès de votre ancien assureur (loi Hamon).",
    },
    {
      question: 'Comment déclarer un sinistre ?',
      reponse:
        "Depuis votre espace client, rubrique « Déclarer un sinistre ». Quelques questions, des photos, et votre dossier est ouvert.",
    },
  ];

  protected readonly formules: Formule[] = [
    {
      nom: 'Essentielle',
      description: 'Les garanties indispensables : dégâts des eaux, incendie, vol et responsabilité civile.',
      plafond: '10 000 €',
      miseEnAvant: false,
      picto: 'picto-essentielle.png',
    },
    {
      nom: 'Confort',
      description: "L'équilibre entre protection et budget, pour la plupart des logements.",
      plafond: '20 000 €',
      miseEnAvant: true,
      picto: 'picto-confort.png',
    },
    {
      nom: 'Sérénité',
      description: 'La protection la plus large, avec remboursement à neuf.',
      plafond: '35 000 €',
      miseEnAvant: false,
      picto: 'picto-serenite.png',
    },
  ];

  protected readonly etapesSinistre: Etape[] = [
    { titre: 'Je me connecte', detail: 'À mon espace client, avec mon code de sécurité.' },
    { titre: 'Je déclare', detail: "Quelques questions selon le type de sinistre, photos à l'appui." },
    {
      titre: 'Je suis mon dossier',
      detail: 'Avancement, pièces demandées et messages avec mon gestionnaire.',
    },
  ];

  protected readonly formulesAuto: FormuleAuto[] = [
    {
      nom: 'Tiers',
      description: "L'essentiel obligatoire : les dommages que vous causez aux autres.",
      miseEnAvant: false,
      picto: 'picto-tiers.png',
    },
    {
      nom: 'Intermédiaire',
      description: 'Le Tiers, plus le vol, l\'incendie, le bris de glace et les événements climatiques.',
      miseEnAvant: true,
      picto: 'picto-intermediaire.png',
    },
    {
      nom: 'Tous risques',
      description: 'Votre véhicule protégé, même en cas d\'accident responsable.',
      miseEnAvant: false,
      picto: 'picto-tous-risques.png',
    },
  ];

  protected choisirOnglet(onglet: 'habitation' | 'auto'): void {
    this.ongletActif.set(onglet);
  }

  protected gererToucheOnglet(event: KeyboardEvent): void {
    if (event.key === 'ArrowRight') {
      this.ongletActif.set(this.ongletActif() === 'habitation' ? 'auto' : 'habitation');
    } else if (event.key === 'ArrowLeft') {
      this.ongletActif.set(this.ongletActif() === 'auto' ? 'habitation' : 'auto');
    }
  }

  protected choisirStatut(statut: Statut): void {
    this.statut.set(statut);
  }

  protected choisirType(type: TypeLogement): void {
    this.typeLogement.set(type);
  }

  protected saisirCodePostal(event: Event): void {
    const champ = event.target as HTMLInputElement;
    const chiffres = champ.value.replace(/\D/g, '').slice(0, 5);
    champ.value = chiffres;
    this.codePostal.set(chiffres);
  }

  protected calculerTarif(event: Event): void {
    event.preventDefault();
    this.tentative.set(true);
    if (this.erreurCodePostal()) {
      this.champCodePostal().nativeElement.focus();
      return;
    }
    if (this.erreurChoix()) {
      return;
    }
    if (this.statut() && this.typeLogement()) {
      this.devisService.prefillDepuisLanding(this.statut()!, this.typeLogement()!, this.codePostal());
    }
    void this.router.navigate(['/devis/logement']);
  }

  protected allerAuDevis(): void {
    void this.router.navigate(['/devis/logement']);
  }

  protected allerADevisAuto(): void {
    void this.router.navigate(['/devis-auto/vehicule']);
  }

  protected allerALaConnexion(): void {
    void this.router.navigate(['/connexion']);
  }

  protected bientotDisponible(service: string): void {
    this.annonce.set(`${service} sera bientôt disponible.`);
  }

  protected fermerAnnonce(): void {
    this.annonce.set(null);
  }

  protected choisirModeAuto(mode: ModeAuto): void {
    this.modeAuto.set(mode);
  }

  protected lancerDevisAuto(event: Event): void {
    event.preventDefault();
    this.tentativeAuto.set(true);
    if (this.erreurModeAuto()) return;
    void this.router.navigate(['/devis-auto/vehicule'], {
      state: { mode: this.modeAuto() },
      queryParams: { mode: this.modeAuto() },
    });
  }

  protected gererToucheChoixAuto(event: KeyboardEvent): void {
    const options: ModeAuto[] = ['plaque', 'marque'];
    const refs = [this.btnPlaque().nativeElement, this.btnMarque().nativeElement];
    const courant = this.modeAuto();
    const index = courant ? options.indexOf(courant) : -1;

    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault();
      const next = index < 0 ? 0 : (index + 1) % options.length;
      this.modeAuto.set(options[next]);
      refs[next].focus();
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault();
      const prev = index < 0 ? options.length - 1 : (index - 1 + options.length) % options.length;
      this.modeAuto.set(options[prev]);
      refs[prev].focus();
    }
  }

  protected basculerQuestion(index: number): void {
    this.questionOuverte.update((ouverte) => (ouverte === index ? -1 : index));
  }
}
