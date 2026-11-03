import { Component, ElementRef, computed, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

export type Statut = 'locataire' | 'proprietaire';
export type TypeLogement = 'appartement' | 'maison';

interface Question {
  question: string;
  reponse: string;
}

interface Formule {
  nom: string;
  description: string;
  plafond: string;
  miseEnAvant: boolean;
}

interface Etape {
  titre: string;
  detail: string;
}

export function codePostalValide(codePostal: string): boolean {
  return /^\d{5}$/.test(codePostal);
}

@Component({
  imports: [RouterLink],
  selector: 'iard-landing',
  styleUrl: './landing.scss',
  templateUrl: './landing.html',
})
export class Landing {
  private readonly champCodePostal = viewChild.required<ElementRef<HTMLInputElement>>('champCodePostal');

  protected readonly statut = signal<Statut | null>(null);
  protected readonly typeLogement = signal<TypeLogement | null>(null);
  protected readonly codePostal = signal('');
  private readonly tentative = signal(false);

  protected readonly erreurChoix = computed(() => this.tentative() && (!this.statut() || !this.typeLogement()));
  protected readonly erreurCodePostal = computed(() => this.tentative() && !codePostalValide(this.codePostal()));

  protected readonly annonce = signal<string | null>(null);

  protected readonly questionOuverte = signal(-1);

  protected readonly questions: Question[] = [
    {
      question: 'Dois-je créer un compte pour connaître mon tarif ?',
      reponse:
        "Non. Votre tarif et la formule conseillée s'affichent sans compte. Il vous sera demandé uniquement pour finaliser la souscription ou enregistrer votre devis.",
    },
    {
      question: 'Je suis déjà assuré ailleurs, comment résilier ?',
      reponse:
        "Si votre contrat a plus d'un an, Assur'Easy s'occupe de la résiliation de votre ancien contrat (loi Hamon). Il suffit d'indiquer le nom de votre assureur actuel.",
    },
    {
      question: 'Quand mon contrat commence-t-il ?',
      reponse: 'Vous choisissez la date de début, entre le lendemain de votre souscription et 90 jours après.',
    },
    {
      question: 'Puis-je changer d’avis après avoir signé ?',
      reponse: 'Oui. Vous disposez de 14 jours après la signature pour renoncer à votre contrat, sans frais.',
    },
  ];

  protected readonly formules: Formule[] = [
    {
      nom: 'Essentielle',
      description: 'Les garanties indispensables : dégâts des eaux, incendie, vol et responsabilité civile.',
      plafond: '10 000 €',
      miseEnAvant: false,
    },
    {
      nom: 'Confort',
      description: "L'équilibre entre protection et budget, pour la plupart des logements.",
      plafond: '20 000 €',
      miseEnAvant: true,
    },
    {
      nom: 'Sérénité',
      description: 'La protection la plus large, avec remboursement à neuf.',
      plafond: '35 000 €',
      miseEnAvant: false,
    },
  ];

  protected readonly etapes: Etape[] = [
    { titre: 'Décrivez votre logement', detail: 'Quelques questions, et seulement celles qui vous concernent.' },
    { titre: 'Recevez notre conseil', detail: 'Une formule recommandée selon vos besoins, avec ses raisons.' },
    {
      titre: 'Ajoutez vos justificatifs',
      detail: "Pièce d'identité et justificatif de domicile, en photo ou en PDF.",
    },
    { titre: 'Signez en ligne', detail: 'Un code reçu par SMS, et votre logement est assuré.' },
  ];

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
    this.bientotDisponible('Le calcul de votre tarif');
  }

  protected bientotDisponible(service: string): void {
    this.annonce.set(`${service} sera bientôt disponible.`);
  }

  protected fermerAnnonce(): void {
    this.annonce.set(null);
  }

  protected basculerQuestion(index: number): void {
    this.questionOuverte.update((ouverte) => (ouverte === index ? -1 : index));
  }
}
