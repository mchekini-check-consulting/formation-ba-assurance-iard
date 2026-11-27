import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LIBELLES_PRODUIT, libelleFormule } from '../../core/model/contrat';
import {
  ADRESSE_SINISTRES,
  AUTRE,
  ChampObligatoire,
  DeclarationSinistre,
  Declarant,
  MESSAGE_AUTRE,
  MESSAGE_BLESSES,
  MESSAGE_HORS_PERIODE,
  MESSAGE_POUR_UN_AUTRE,
  Question,
  ReponsesSinistre,
  Tiers,
  aujourdhuiIso,
  declarantsPossibles,
  erreursCirconstances,
  messageDelai,
  messageNonCouvert,
  questionsDuType,
  reponsesDe,
} from '../../core/model/sinistre';
import { SinistreService } from '../../core/service/sinistre.service';
import { MESSAGE_ENREGISTRE } from '../declarer-sinistre/declarer-sinistre';
import { BandeauUrgence } from '../sinistre-commun/bandeau-urgence';
import { EtapesSinistre } from '../sinistre-commun/etapes-sinistre';

export const MESSAGE_CONTINUER = 'Vérifiez les informations ci-dessus, puis cliquez à nouveau sur « Suivant » pour continuer.';

/**
 * US-24, étape 2 « Circonstances » (maquettes « Déclarer – 2 » et « Déclaration simplifiée » pour un proche).
 * AC4 : date (jamais dans le futur), heure, lieu, description, questions propres au type, tiers et blessés. AC5, AC6
 * et AC3 (garanties à la date du sinistre, RG4) : messages neutres après « Suivant », jamais bloquants ; en retard,
 * l'explication devient obligatoire. AC11 : blessés oui / non et leur nombre, sans aucun champ médical. AC13 : qui
 * déclare, sans présélection ; nom et lien obligatoires pour un proche ou le conducteur secondaire.
 */
@Component({
  selector: 'iard-sinistre-circonstances',
  imports: [RouterLink, BandeauUrgence, EtapesSinistre],
  styleUrls: ['../mes-devis/mes-devis.scss', '../sinistre-commun/sinistre.scss'],
  templateUrl: './sinistre-circonstances.html',
})
export class SinistreCirconstances {
  private readonly sinistres = inject(SinistreService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly declaration = signal<DeclarationSinistre | null>(null);
  protected readonly reponses = signal<ReponsesSinistre | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly erreurs = signal<Partial<Record<ChampObligatoire, string>>>({});
  protected readonly enCours = signal(false);
  protected readonly messageAction = signal<string | null>(null);
  /** Les messages AC3, AC5, AC6 ont été montrés pour ces réponses : « Suivant » continue. */
  protected readonly vus = signal(false);
  protected readonly aujourdhui = aujourdhuiIso();

  protected readonly questions = computed<Question[]>(() => {
    const declaration = this.declaration();
    return declaration ? questionsDuType(declaration.contrat.produit, declaration.type) : [];
  });
  protected readonly declarants = computed(() => {
    const declaration = this.declaration();
    return declaration ? declarantsPossibles(declaration.contrat.produit) : [];
  });
  protected readonly controles = computed(() => (this.vus() ? (this.declaration()?.controles ?? null) : null));
  protected readonly messageCouverture = computed(() => {
    const declaration = this.declaration();
    return this.controles()?.couvert === false && declaration
      ? messageNonCouvert(libelleFormule(declaration.contrat.formule))
      : null;
  });
  protected readonly aDesErreurs = computed(() => Object.keys(this.erreurs()).length > 0);
  protected readonly pourUnAutre = computed(() => {
    const declarant = this.reponses()?.declarant;
    return !!declarant && declarant !== 'SOUSCRIPTEUR';
  });

  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly AUTRE = AUTRE;
  protected readonly ADRESSE_SINISTRES = ADRESSE_SINISTRES;
  protected readonly MESSAGE_AUTRE = MESSAGE_AUTRE;
  protected readonly MESSAGE_BLESSES = MESSAGE_BLESSES;
  protected readonly MESSAGE_HORS_PERIODE = MESSAGE_HORS_PERIODE;
  protected readonly MESSAGE_POUR_UN_AUTRE = MESSAGE_POUR_UN_AUTRE;
  protected readonly MESSAGE_CONTINUER = MESSAGE_CONTINUER;
  protected readonly messageDelai = messageDelai;

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((parametres) => {
      this.sinistres.lire(Number(parametres.get('id'))).subscribe((resultat) => {
        if (resultat.statut !== 'ok') {
          this.erreur.set(resultat.message);
          return;
        }
        this.charger(resultat.valeur);
        // Brouillon repris : les messages déjà calculés sont affichés tout de suite.
        this.vus.set(this.aDesMessages(resultat.valeur));
      });
    });
  }

  private charger(declaration: DeclarationSinistre): void {
    this.declaration.set(declaration);
    this.reponses.set(reponsesDe(declaration));
  }

  private aDesMessages(declaration: DeclarationSinistre): boolean {
    const { couvert, horsPeriodeContrat, horsDelai } = declaration.controles;
    return couvert === false || horsPeriodeContrat || horsDelai;
  }

  /** Met à jour une réponse ; changer une date fait recalculer les messages au prochain « Suivant ». */
  protected modifier<K extends keyof ReponsesSinistre>(champ: K, valeur: ReponsesSinistre[K]): void {
    const reponses = this.reponses();
    if (!reponses) return;
    this.reponses.set({ ...reponses, [champ]: valeur });
    if (champ === 'dateSurvenance' || champ === 'dateArreteCatnat') this.vus.set(false);
    if (champ in this.erreurs()) {
      const restantes = { ...this.erreurs() };
      delete restantes[champ as ChampObligatoire];
      this.erreurs.set(restantes);
    }
  }

  protected texte(champ: 'lieu' | 'description' | 'explicationRetard' | 'declarantNom' | 'declarantLien', evenement: Event): void {
    this.modifier(champ, (evenement.target as HTMLInputElement).value || null);
  }

  protected date(champ: 'dateSurvenance' | 'dateArreteCatnat' | 'heureSurvenance', evenement: Event): void {
    this.modifier(champ, (evenement.target as HTMLInputElement).value || null);
  }

  protected nombre(evenement: Event): void {
    const valeur = Number((evenement.target as HTMLInputElement).value);
    this.modifier('nombreBlesses', Number.isInteger(valeur) && valeur > 0 ? valeur : null);
  }

  protected tiers(champ: keyof Tiers, evenement: Event): void {
    const actuel = this.reponses()?.tiers ?? { nom: null, immatriculation: null, assureur: null };
    this.modifier('tiers', { ...actuel, [champ]: (evenement.target as HTMLInputElement).value || null });
  }

  protected detail(question: string, valeur: string): void {
    const details: Record<string, string> = { ...this.reponses()?.details };
    if (valeur) {
      details[question] = valeur;
    } else {
      delete details[question];
    }
    this.modifier('details', details);
  }

  protected declarant(valeur: Declarant): void {
    this.modifier('declarant', valeur);
  }

  /** AC4 à AC6 : contrôle des réponses obligatoires, enregistrement, messages, puis étape 3. */
  protected suivant(): void {
    const reponses = this.reponses();
    const declaration = this.declaration();
    if (!reponses || !declaration || this.enCours()) return;
    const explication = this.vus() && declaration.controles.horsDelai;
    const erreurs = erreursCirconstances(reponses, this.aujourdhui, explication);
    this.erreurs.set(erreurs);
    if (Object.keys(erreurs).length > 0) return;

    this.enregistrer(2, (enregistree) => {
      const montrer = this.aDesMessages(enregistree) && !this.vus();
      this.vus.set(this.aDesMessages(enregistree));
      if (montrer) return;
      if (enregistree.controles.horsDelai && !enregistree.explicationRetard) {
        this.erreurs.set(erreursCirconstances(reponsesDe(enregistree), this.aujourdhui, true));
        return;
      }
      this.enregistrer(3, () => {
        void this.router.navigate([ADRESSE_SINISTRES, String(enregistree.id), 'justificatifs']);
      });
    });
  }

  protected precedent(): void {
    this.enregistrer(1, (enregistree) => {
      void this.router.navigate([ADRESSE_SINISTRES, String(enregistree.id), 'type']);
    });
  }

  /** AC8 : « Enregistrer et terminer plus tard » (les réponses peuvent être incomplètes). */
  protected plusTard(): void {
    this.enregistrer(2, () => {
      void this.router.navigate([ADRESSE_SINISTRES], { state: { message: MESSAGE_ENREGISTRE } });
    });
  }

  private enregistrer(etape: number, ensuite: (declaration: DeclarationSinistre) => void): void {
    const reponses = this.reponses();
    const declaration = this.declaration();
    if (!reponses || !declaration) return;
    this.enCours.set(true);
    this.messageAction.set(null);
    this.sinistres.enregistrer(declaration.id, { ...reponses, etape }).subscribe((resultat) => {
      this.enCours.set(false);
      if (resultat.statut !== 'ok') {
        this.messageAction.set(resultat.message);
        return;
      }
      this.declaration.set(resultat.valeur);
      ensuite(resultat.valeur);
    });
  }
}
