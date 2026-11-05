import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  Civilite,
  CleZone,
  IdentiteCompte,
  SaisieInformations,
  TYPES_JUSTIFICATIF_DOMICILE,
  TYPES_PIECE_IDENTITE,
  ZoneDepot,
  dateDepuisIso,
  dateVersIso,
  erreurDateNaissance,
  erreurFichier,
  erreurTelephone,
  formaterSaisieDate,
  nombreAutresColocataires,
  normaliserTelephone,
} from '../../core/model/informations';
import { DevisService } from '../../core/service/devis.service';
import { InformationsService, ResultatInformations } from '../../core/service/informations.service';
import { DevisEntete } from '../devis-entete/devis-entete';

const ZONES = [
  {
    cle: 'pieceIdentite',
    id: 'identite',
    titre: "Pièce d'identité",
    types: TYPES_PIECE_IDENTITE,
    manquant: "Déposez votre pièce d'identité.",
  },
  {
    cle: 'domicile',
    id: 'domicile',
    titre: 'Justificatif de domicile',
    types: TYPES_JUSTIFICATIF_DOMICILE,
    manquant: 'Déposez votre justificatif de domicile.',
  },
] as const;

const ZONE_DU_CHAMP: Readonly<Record<string, (typeof ZONES)[number]>> = {
  pieceIdentite: ZONES[0],
  justificatifDomicile: ZONES[1],
};

@Component({
  selector: 'iard-devis-informations',
  imports: [DevisEntete, RouterLink],
  styleUrls: [
    '../devis-logement/devis-logement.scss',
    '../devis-besoins/devis-besoins.scss',
    './devis-informations.scss',
  ],
  templateUrl: './devis-informations.html',
})
export class DevisInformations {
  private readonly devis = inject(DevisService);
  private readonly informations = inject(InformationsService);
  private readonly router = inject(Router);
  private readonly page = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  private readonly tentative = signal(false);

  protected readonly zones = ZONES;
  protected readonly saisie = this.devis.informations;
  protected readonly envoi = signal(false);
  protected readonly erreurEnvoi = signal(false);

  // AC4
  protected readonly nomsColocataires = computed(() => {
    const { statut, nombreColocataires } = this.devis.logement();
    const nombre = statut === 'colocation' ? nombreAutresColocataires(nombreColocataires) : 0;
    return Array.from({ length: nombre }, (_, i) => this.saisie().colocataires[i] ?? '');
  });

  /** Tous les contrôles, dans l'ordre de l'écran : la première erreur reçoit le curseur (AC5). */
  private readonly controles = computed(() => {
    const s = this.saisie();
    return new Map<string, string | null>([
      ['civilite', s.civilite ? null : 'Indiquez votre civilité.'],
      ['nom', s.nom.trim() ? null : 'Indiquez votre nom.'],
      ['prenom', s.prenom.trim() ? null : 'Indiquez votre prénom.'],
      ['date-naissance', erreurDateNaissance(s.dateNaissance, new Date())],
      ['telephone', erreurTelephone(s.telephone)],
      ...this.controlesZones(s),
      ...this.nomsColocataires().map((nom, i): [string, string | null] => [
        `colocataire-${i}`,
        nom.trim() ? null : 'Indiquez le nom et le prénom de ce colocataire.',
      ]),
    ]);
  });

  constructor() {
    this.informations.lire().subscribe((identite) => {
      if (identite) this.preRemplir(identite);
    });
  }

  protected erreur(id: string): string | null {
    return this.tentative() ? (this.controles().get(id) ?? null) : null;
  }

  // AC3 : un fichier refusé est signalé dès le dépôt, sans attendre le bouton
  protected erreurDepot(zone: (typeof ZONES)[number]): string | null {
    return this.saisie()[zone.cle].erreur ?? this.erreur(`fichier-${zone.id}`);
  }

  protected choisirCivilite(civilite: Civilite): void {
    this.mettreAJour({ civilite });
  }

  protected saisirTexte(champ: 'nom' | 'prenom' | 'telephone', e: Event): void {
    this.mettreAJour({ [champ]: (e.target as HTMLInputElement).value });
  }

  protected saisirDate(e: Event): void {
    const champ = e.target as HTMLInputElement;
    champ.value = formaterSaisieDate(champ.value);
    this.mettreAJour({ dateNaissance: champ.value });
  }

  protected choisirType(cle: CleZone, e: Event): void {
    this.mettreAJourZone(cle, { type: (e.target as HTMLSelectElement).value });
  }

  protected saisirColocataire(indice: number, e: Event): void {
    const noms = [...this.nomsColocataires()];
    noms[indice] = (e.target as HTMLInputElement).value;
    this.mettreAJour({ colocataires: noms });
  }

  protected async choisirFichier(cle: CleZone, e: Event): Promise<void> {
    const champ = e.target as HTMLInputElement;
    const fichier = champ.files?.[0];
    if (fichier) await this.deposer(cle, fichier);
    champ.value = '';
  }

  protected async lacherFichier(cle: CleZone, e: DragEvent): Promise<void> {
    e.preventDefault();
    const fichier = e.dataTransfer?.files[0];
    if (fichier) await this.deposer(cle, fichier);
  }

  protected continuer(e: Event): void {
    e.preventDefault();
    if (this.envoi()) return;
    this.tentative.set(true);
    this.erreurEnvoi.set(false);
    const premiereErreur = [...this.controles()].find(([, message]) => message)?.[0];
    if (premiereErreur) {
      this.placerCurseur(premiereErreur);
      return;
    }
    this.envoyer();
  }

  private controlesZones(s: SaisieInformations): [string, string | null][] {
    return ZONES.flatMap(({ cle, id, manquant }): [string, string | null][] => [
      [`type-${id}`, s[cle].type ? null : 'Choisissez le type de document.'],
      [`fichier-${id}`, s[cle].erreur ?? (s[cle].fichier ? null : manquant)],
    ]);
  }

  // AC1 : seul ce que le client a déjà saisi est repris, sans écraser une saisie en cours
  private preRemplir(identite: IdentiteCompte): void {
    this.saisie.update((s) => ({
      ...s,
      civilite: s.civilite ?? identite.civilite,
      nom: s.nom || (identite.nom ?? ''),
      prenom: s.prenom || (identite.prenom ?? ''),
      dateNaissance: s.dateNaissance || (identite.dateNaissance ? dateDepuisIso(identite.dateNaissance) : ''),
      telephone: s.telephone || (identite.telephone ?? ''),
    }));
  }

  private mettreAJour(partiel: Partial<SaisieInformations>): void {
    this.saisie.update((s) => ({ ...s, ...partiel }));
  }

  private mettreAJourZone(cle: CleZone, partiel: Partial<ZoneDepot>): void {
    this.saisie.update((s) => ({ ...s, [cle]: { ...s[cle], ...partiel } }));
  }

  private async deposer(cle: CleZone, fichier: File): Promise<void> {
    const erreur = await erreurFichier(fichier);
    this.mettreAJourZone(cle, { fichier: erreur ? null : fichier, erreur });
  }

  private placerCurseur(id: string): void {
    this.page.querySelector<HTMLElement>(`#${id}`)?.focus();
  }

  private envoyer(): void {
    const s = this.saisie();
    this.envoi.set(true);
    this.informations
      .enregistrer(
        {
          referenceDevis: this.devis.reference() as string,
          civilite: s.civilite as string,
          nom: s.nom,
          prenom: s.prenom,
          dateNaissance: dateVersIso(s.dateNaissance),
          telephone: normaliserTelephone(s.telephone),
          typePieceIdentite: s.pieceIdentite.type,
          typeJustificatifDomicile: s.domicile.type,
          colocataires: this.nomsColocataires(),
        },
        s.pieceIdentite.fichier as File,
        s.domicile.fichier as File,
      )
      .subscribe((resultat) => {
        this.envoi.set(false);
        this.traiterResultat(resultat);
      });
  }

  private traiterResultat(resultat: ResultatInformations): void {
    if (resultat.statut === 'enregistre') {
      void this.router.navigate(['/devis/paiement']);
    } else if (resultat.statut === 'session-expiree') {
      void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis/informations' } });
    } else if (resultat.statut === 'refuse' && ZONE_DU_CHAMP[resultat.champ]) {
      const zone = ZONE_DU_CHAMP[resultat.champ];
      this.mettreAJourZone(zone.cle, { fichier: null, erreur: resultat.message });
      this.placerCurseur(`fichier-${zone.id}`);
    } else {
      this.erreurEnvoi.set(true);
    }
  }
}
