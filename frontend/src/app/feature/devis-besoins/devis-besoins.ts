import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  FORMULES,
  ReponsesBesoins,
  depassePlafondSerenite,
  formaterPrix,
  prixFormule,
  reponsesCompletes,
} from '../../core/model/recommandation';
import { DevisService } from '../../core/service/devis.service';
import { RecommandationService } from '../../core/service/recommandation.service';
import { DevisEntete } from '../devis-entete/devis-entete';

type CleChoix = Exclude<keyof ReponsesBesoins, 'valeurContenu'>;

interface QuestionChoix {
  cle: CleChoix;
  libelle: string;
  aide: string;
  choix: { valeur: boolean | string; libelle: string }[];
}

const OUI_NON = [
  { valeur: true, libelle: 'Oui' },
  { valeur: false, libelle: 'Non' },
];

@Component({
  selector: 'iard-devis-besoins',
  imports: [DevisEntete, RouterLink],
  styleUrls: ['../devis-logement/devis-logement.scss', './devis-besoins.scss'],
  templateUrl: './devis-besoins.html',
})
export class DevisBesoins {
  private readonly devis = inject(DevisService);
  private readonly recommandationService = inject(RecommandationService);
  private readonly router = inject(Router);

  protected readonly questions: QuestionChoix[] = [
    {
      cle: 'objetsValeur',
      libelle: 'Possédez-vous des objets de valeur ?',
      aide: "Bijoux, œuvres d'art, collections…",
      choix: OUI_NON,
    },
    {
      cle: 'appareilsCouteux',
      libelle: 'Possédez-vous des appareils coûteux ?',
      aide: 'Ordinateur, télévision, matériel photo…',
      choix: OUI_NON,
    },
    {
      cle: 'remboursement',
      libelle: 'Comment souhaitez-vous être remboursé ?',
      aide: "Vétusté déduite : le remboursement tient compte de l'usure du bien.",
      choix: [
        { valeur: 'neuf', libelle: 'À neuf' },
        { valeur: 'vetuste', libelle: 'Vétusté déduite' },
      ],
    },
    {
      cle: 'assistance',
      libelle: 'Souhaitez-vous une assistance dépannage ?',
      aide: 'Serrurier, plombier ou électricien en cas d’urgence.',
      choix: OUI_NON,
    },
    {
      cle: 'litiges',
      libelle: 'Souhaitez-vous être accompagné en cas de litige ?',
      aide: 'Avec un voisin, un propriétaire ou un artisan, par exemple.',
      choix: OUI_NON,
    },
    {
      cle: 'priorite',
      libelle: 'Quelle est votre priorité ?',
      aide: 'Votre réponse oriente notre conseil.',
      choix: [
        { valeur: 'prix', libelle: 'Le prix' },
        { valeur: 'equilibre', libelle: "L'équilibre" },
        { valeur: 'protection', libelle: 'La protection la plus large' },
      ],
    },
  ];

  protected readonly reponses = this.devis.besoins;
  protected readonly recommandation = this.devis.recommandation;
  protected readonly envoi = signal(false);
  protected readonly erreur = signal(false);

  protected readonly valeurContenu = computed(() => this.reponses().valeurContenu?.toString() ?? '');
  protected readonly complet = computed(() => reponsesCompletes(this.reponses()));
  protected readonly nomFormule = computed(() => {
    const recommandation = this.recommandation();
    return recommandation && FORMULES[recommandation.formule].nom;
  });
  protected readonly prix = computed(() => {
    const recommandation = this.recommandation();
    const tarif = this.devis.tarifBase();
    return recommandation && tarif !== null ? formaterPrix(prixFormule(tarif, recommandation.formule)) : null;
  });
  protected readonly depassePlafond = computed(() => depassePlafondSerenite(this.reponses()));

  protected saisirContenu(e: Event): void {
    const champ = e.target as HTMLInputElement;
    const valeur = champ.value.replace(/\D/g, '').slice(0, 7);
    champ.value = valeur;
    this.repondre({ valeurContenu: valeur === '' ? null : Number(valeur) });
  }

  protected choisir(cle: CleChoix, valeur: boolean | string): void {
    this.repondre({ [cle]: valeur });
  }

  protected voirRecommandation(e: Event): void {
    e.preventDefault();
    const recommandation = this.recommandation();
    if (!this.complet() || !recommandation || this.envoi()) return;

    this.envoi.set(true);
    this.erreur.set(false);
    this.recommandationService.enregistrer(this.reponses(), recommandation).subscribe((reference) => {
      this.envoi.set(false);
      if (reference === null) {
        this.erreur.set(true);
        return;
      }
      this.devis.validerRecommandation(reference, recommandation.formule);
      void this.router.navigate(['/devis/formule']);
    });
  }

  private repondre(reponse: Partial<ReponsesBesoins>): void {
    this.erreur.set(false);
    this.devis.mettreAJourBesoins(reponse);
  }
}
