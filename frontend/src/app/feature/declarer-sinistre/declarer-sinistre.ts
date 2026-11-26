import { Location } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LIBELLES_PRODUIT, libelleFormule } from '../../core/model/contrat';
import { formaterNomPropre } from '../../core/model/identite';
import {
  ADRESSE_SINISTRES,
  AUTRE,
  ContratDeclarable,
  DeclarationSinistre,
  MESSAGE_AUTRE,
  TypeSinistre,
  TypesSinistre,
  messageNonCouvert,
  reponsesDe,
} from '../../core/model/sinistre';
import { SinistreService } from '../../core/service/sinistre.service';
import { BandeauUrgence } from '../sinistre-commun/bandeau-urgence';
import { EtapesSinistre } from '../sinistre-commun/etapes-sinistre';

export const MESSAGE_ENREGISTRE =
  'Votre brouillon est enregistré. Vous le retrouvez ici pendant 30 jours pour terminer votre déclaration.';
export const MESSAGE_AUCUN_CONTRAT =
  'Aucun de vos contrats ne permet de déclarer un sinistre en ligne. Appelez notre service d’assistance pour être accompagné.';

/**
 * US-24, étape 1 « Que s'est-il passé ? » (maquette « Déclarer – 1 »). AC1 : choix du contrat parmi ceux ouverts à la
 * déclaration, jamais présélectionné (même s'il n'y en a qu'un) ; depuis le raccourci d'un contrat, ce contrat est
 * celui que le client a choisi. AC2 : les types du produit en tuiles, sans présélection, « Autre » en dernier.
 * AC8 : choisir un type enregistre le brouillon ; AC3 : message si la formule ne couvre pas ce type (jamais bloquant).
 */
@Component({
  selector: 'iard-declarer-sinistre',
  imports: [RouterLink, BandeauUrgence, EtapesSinistre],
  styleUrls: ['../mes-devis/mes-devis.scss', '../sinistre-commun/sinistre.scss'],
  templateUrl: './declarer-sinistre.html',
})
export class DeclarerSinistre {
  private readonly sinistres = inject(SinistreService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);

  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly prenom = signal('');
  protected readonly numeroAssistance = signal('');
  protected readonly contrats = signal<ContratDeclarable[]>([]);
  protected readonly contratChoisi = signal<ContratDeclarable | null>(null);
  protected readonly types = signal<TypesSinistre | null>(null);
  protected readonly declaration = signal<DeclarationSinistre | null>(null);
  protected readonly enCours = signal(false);
  protected readonly messageAction = signal<string | null>(null);

  protected readonly typeChoisi = computed(() => this.declaration()?.type ?? null);
  protected readonly typeCourant = computed<TypeSinistre | null>(
    () => this.types()?.types.find((type) => type.code === this.typeChoisi()) ?? null,
  );
  /** AC3 : jamais pour « Autre » (couvert vaut alors null). */
  protected readonly messageCouverture = computed(() => {
    const declaration = this.declaration();
    if (!declaration || declaration.controles.couvert !== false) return null;
    return messageNonCouvert(libelleFormule(declaration.contrat.formule));
  });

  protected readonly LIBELLES_PRODUIT = LIBELLES_PRODUIT;
  protected readonly libelleFormule = libelleFormule;
  protected readonly AUTRE = AUTRE;
  protected readonly ADRESSE_SINISTRES = ADRESSE_SINISTRES;
  protected readonly MESSAGE_AUCUN_CONTRAT = MESSAGE_AUCUN_CONTRAT;
  protected readonly MESSAGE_AUTRE = MESSAGE_AUTRE;

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((parametres) => {
      const id = Number(parametres.get('id'));
      if (id) {
        this.chargerBrouillon(id);
      } else {
        this.chargerContexte(this.route.snapshot.queryParamMap.get('contrat'));
      }
    });
  }

  private chargerContexte(numeroDemande: string | null): void {
    this.sinistres.contexte().subscribe((resultat) => {
      this.chargement.set(false);
      if (resultat.statut !== 'ok') {
        this.erreur.set(resultat.message);
        return;
      }
      this.prenom.set(formaterNomPropre(resultat.valeur.prenom));
      this.numeroAssistance.set(resultat.valeur.numeroAssistance);
      this.contrats.set(resultat.valeur.contrats);
      const demande = resultat.valeur.contrats.find((contrat) => contrat.numero === numeroDemande);
      if (demande) this.choisirContrat(demande);
    });
  }

  private chargerBrouillon(id: number): void {
    this.sinistres.lire(id).subscribe((resultat) => {
      this.chargement.set(false);
      if (resultat.statut !== 'ok') {
        this.erreur.set(resultat.message);
        return;
      }
      const declaration = resultat.valeur;
      this.declaration.set(declaration);
      this.prenom.set(formaterNomPropre(declaration.prenom));
      this.numeroAssistance.set(declaration.numeroAssistance);
      this.contrats.set([declaration.contrat]);
      this.contratChoisi.set(declaration.contrat);
      this.chargerTypes(declaration.contrat.numero);
    });
  }

  /** AC1 : le contrat d'un brouillon déjà commencé ne change plus. */
  protected choisirContrat(contrat: ContratDeclarable): void {
    if (this.declaration() || this.contratChoisi()?.numero === contrat.numero) return;
    this.contratChoisi.set(contrat);
    this.types.set(null);
    this.chargerTypes(contrat.numero);
  }

  private chargerTypes(numero: string): void {
    this.sinistres.types(numero).subscribe((resultat) => {
      if (resultat.statut === 'ok') {
        this.types.set(resultat.valeur);
        this.numeroAssistance.set(resultat.valeur.numeroAssistance);
      } else {
        this.messageAction.set(resultat.message);
      }
    });
  }

  /** AC2, AC3, AC8 : le type choisi est enregistré tout de suite (création du brouillon la première fois). */
  protected choisirType(type: TypeSinistre): void {
    const contrat = this.contratChoisi();
    if (!contrat || this.enCours() || type.code === this.typeChoisi()) return;
    this.enCours.set(true);
    this.messageAction.set(null);
    const declaration = this.declaration();
    const appel = declaration
      ? this.sinistres.enregistrer(declaration.id, { ...reponsesDe(declaration), type: type.code, etape: 1 })
      : this.sinistres.creer(contrat.numero, type.code);
    appel.subscribe((resultat) => {
      this.enCours.set(false);
      if (resultat.statut !== 'ok') {
        this.messageAction.set(resultat.message);
        return;
      }
      this.declaration.set(resultat.valeur);
      // Sans recharger la page : l'adresse devient celle du brouillon (retrouvé après un rechargement).
      this.location.replaceState(`${ADRESSE_SINISTRES}/${resultat.valeur.id}/type`);
    });
  }

  protected suivant(): void {
    this.enregistrerPuis(2, [ADRESSE_SINISTRES, String(this.declaration()?.id), 'circonstances']);
  }

  /** AC8 : « Enregistrer et terminer plus tard ». */
  protected plusTard(): void {
    this.enregistrerPuis(1, [ADRESSE_SINISTRES], MESSAGE_ENREGISTRE);
  }

  private enregistrerPuis(etape: number, lien: string[], message?: string): void {
    const declaration = this.declaration();
    if (!declaration || this.enCours()) return;
    this.enCours.set(true);
    this.sinistres.enregistrer(declaration.id, { ...reponsesDe(declaration), etape }).subscribe((resultat) => {
      this.enCours.set(false);
      if (resultat.statut === 'ok') {
        void this.router.navigate(lien, message ? { state: { message } } : undefined);
      } else {
        this.messageAction.set(resultat.message);
      }
    });
  }
}
