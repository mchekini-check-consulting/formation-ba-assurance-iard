import { TitleCasePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HistoriqueEntree, Permission, ProfilInfo, ProfilsService } from '../../core/service/profils.service';

const LABELS_RUBRIQUES: Record<string, string> = {
  DEVIS: 'Devis',
  SOUSCRIPTION: 'Souscription',
  CONTRATS: 'Contrats',
  DOCUMENTS: 'Documents',
  SINISTRES: 'Sinistres',
  COMPTES_UTILISATEURS: 'Comptes utilisateurs',
  PROFILS: 'Profils',
};

const LABELS_DROITS: Record<string, string> = {
  CONSULTER: 'Consulter',
  CREER: 'Créer',
  MODIFIER: 'Modifier',
  DESACTIVER: 'Désactiver',
};

const ORDRE_RUBRIQUES = ['DEVIS', 'SOUSCRIPTION', 'CONTRATS', 'DOCUMENTS', 'SINISTRES', 'COMPTES_UTILISATEURS', 'PROFILS'];
const ORDRE_DROITS = ['CONSULTER', 'CREER', 'MODIFIER', 'DESACTIVER'];

/** US-02 : gestion des profils et de leurs droits d'accès (réservé aux administrateurs). */
@Component({
  selector: 'iard-gestion-profils',
  imports: [RouterLink, TitleCasePipe],
  styleUrl: './gestion-profils.scss',
  templateUrl: './gestion-profils.html',
})
export class GestionProfils {
  private readonly service = inject(ProfilsService);

  protected readonly profils = signal<ProfilInfo[] | null>(null);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal<string | null>(null);
  protected readonly profilActif = signal<string | null>(null);
  protected readonly permissionsModifiees = signal<Map<string, boolean>>(new Map());
  protected readonly enregistrement = signal(false);
  protected readonly confirmation = signal(false);
  protected readonly historique = signal<HistoriqueEntree[] | null>(null);
  protected readonly afficherHistorique = signal(false);

  protected readonly LABELS_RUBRIQUES = LABELS_RUBRIQUES;
  protected readonly LABELS_DROITS = LABELS_DROITS;
  protected readonly ORDRE_RUBRIQUES = ORDRE_RUBRIQUES;
  protected readonly ORDRE_DROITS = ORDRE_DROITS;

  constructor() {
    this.service.lister().subscribe({
      next: (profils) => {
        this.chargement.set(false);
        this.profils.set(profils);
        if (profils.length > 0) {
          this.selectionnerProfil(profils[0].nom);
        }
      },
      error: () => {
        this.chargement.set(false);
        this.erreur.set("Les profils n'ont pas pu être chargés. Réessayez dans quelques instants.");
      },
    });
  }

  protected selectionnerProfil(nom: string): void {
    this.profilActif.set(nom);
    this.permissionsModifiees.set(new Map());
    this.confirmation.set(false);
    this.historique.set(null);
    this.afficherHistorique.set(false);
    this.erreur.set(null);
  }

  protected profilCourant(): ProfilInfo | null {
    return this.profils()?.find((p) => p.nom === this.profilActif()) ?? null;
  }

  protected valeurPermission(rubrique: string, droit: string): boolean {
    const cle = `${rubrique}_${droit}`;
    const modifiee = this.permissionsModifiees().get(cle);
    if (modifiee !== undefined) {
      return modifiee;
    }
    return this.profilCourant()?.permissions.find((p) => p.rubrique === rubrique && p.droit === droit)?.valeur ?? false;
  }

  protected estVerrouille(rubrique: string): boolean {
    return this.profilActif() === 'ADMINISTRATEUR' && rubrique === 'PROFILS';
  }

  protected basculerPermission(rubrique: string, droit: string): void {
    if (this.estVerrouille(rubrique)) {
      return;
    }
    const cle = `${rubrique}_${droit}`;
    const actuelle = this.valeurPermission(rubrique, droit);
    const modifiees = new Map(this.permissionsModifiees());
    const originale = this.profilCourant()?.permissions.find((p) => p.rubrique === rubrique && p.droit === droit)?.valeur ?? false;
    if (actuelle !== originale) {
      modifiees.delete(cle);
    } else {
      modifiees.set(cle, !actuelle);
    }
    this.permissionsModifiees.set(modifiees);
  }

  protected aDesModifications(): boolean {
    return this.permissionsModifiees().size > 0;
  }

  protected demanderConfirmation(): void {
    this.confirmation.set(true);
  }

  protected annulerConfirmation(): void {
    this.confirmation.set(false);
  }

  protected confirmerEnregistrement(): void {
    const profil = this.profilActif();
    const courant = this.profilCourant();
    if (!profil || !courant) {
      return;
    }
    const permissions: Permission[] = courant.permissions.map((p) => ({
      rubrique: p.rubrique,
      droit: p.droit,
      valeur: this.valeurPermission(p.rubrique, p.droit),
    }));
    this.enregistrement.set(true);
    this.erreur.set(null);
    this.service.mettreAJourPermissions(profil, permissions).subscribe({
      next: () => {
        this.enregistrement.set(false);
        this.confirmation.set(false);
        this.permissionsModifiees.set(new Map());
        this.profils.update((liste) => {
          if (!liste) return liste;
          return liste.map((p) =>
            p.nom === profil ? { ...p, permissions } : p
          );
        });
      },
      error: () => {
        this.enregistrement.set(false);
        this.erreur.set("Les droits n'ont pas pu être enregistrés. Réessayez dans quelques instants.");
      },
    });
  }

  protected afficherOuMasquerHistorique(): void {
    if (this.afficherHistorique()) {
      this.afficherHistorique.set(false);
      return;
    }
    const profil = this.profilActif();
    if (!profil) return;
    this.service.lireHistorique(profil).subscribe({
      next: (historique) => {
        this.historique.set(historique);
        this.afficherHistorique.set(true);
      },
      error: () => {
        this.erreur.set("L'historique n'a pas pu être chargé.");
      },
    });
  }

  protected formaterDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
