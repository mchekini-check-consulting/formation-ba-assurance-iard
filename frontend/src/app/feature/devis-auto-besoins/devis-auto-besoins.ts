import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { BesoinsAuto, PrioriteAuto } from '../../core/model/devis-auto';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { BesoinsAutoService, besoinsVersRequete, reponseVersRecommandation } from '../../core/service/besoins-auto.service';
import { DevisAutoLayout } from '../devis-auto-layout/devis-auto-layout';

@Component({
  selector: 'iard-devis-auto-besoins',
  imports: [RouterLink, DevisAutoLayout],
  templateUrl: './devis-auto-besoins.html',
  styleUrl: './devis-auto-besoins.scss',
})
export class DevisAutoBesoins {
  protected readonly devisAutoService = inject(DevisAutoService);
  private readonly besoinsService = inject(BesoinsAutoService);
  private readonly router = inject(Router);

  private readonly saisieExistante = this.devisAutoService.besoins();

  protected readonly valeurVehicule = signal<number | null>(
    this.saisieExistante?.valeurVehicule ?? null,
  );
  protected readonly vehiculeIndispensable = signal<boolean | null>(
    this.saisieExistante?.vehiculeIndispensable ?? null,
  );
  protected readonly assistance0km = signal<boolean>(
    this.saisieExistante?.assistance0km ?? false,
  );
  protected readonly protectionConducteur = signal<boolean>(
    this.saisieExistante?.protectionConducteur ?? false,
  );
  protected readonly brisGlace = signal<boolean>(
    this.saisieExistante?.brisGlace ?? false,
  );
  protected readonly accompagnementLitige = signal<boolean>(
    this.saisieExistante?.accompagnementLitige ?? false,
  );
  protected readonly priorite = signal<PrioriteAuto | null>(
    this.saisieExistante?.priorite ?? null,
  );

  protected readonly tentative = signal(false);
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  protected readonly complet = computed(() =>
    this.valeurVehicule() !== null &&
    this.vehiculeIndispensable() !== null &&

    this.priorite() !== null,
  );

  protected readonly tarifEtape1 = this.devisAutoService.tarifEtape1;

  protected readonly erreurValeur = computed(() => {
    if (!this.tentative()) return null;
    return this.valeurVehicule() === null ? 'Indiquez la valeur de votre véhicule.' : null;
  });
  protected readonly erreurIndispensable = computed(() =>
    this.tentative() && this.vehiculeIndispensable() === null
      ? 'Répondez à cette question.'
      : null,
  );
  protected readonly erreurPriorite = computed(() =>
    this.tentative() && this.priorite() === null ? 'Choisissez votre priorité.' : null,
  );

  protected saisirValeur(e: Event): void {
    const raw = (e.target as HTMLInputElement).value.replace(/\s/g, '');
    const v = parseInt(raw, 10);
    this.valeurVehicule.set(isNaN(v) ? null : v);
  }

  protected soumettre(e: Event): void {
    e.preventDefault();
    this.tentative.set(true);
    this.erreur.set(null);

    if (!this.complet()) {
      return;
    }

    const id = this.devisAutoService.devisAutoId();
    if (!id) {
      this.erreur.set('Identifiant du devis introuvable. Revenez à l\'étape précédente.');
      return;
    }

    const besoins: BesoinsAuto = {
      valeurVehicule: this.valeurVehicule()!,
      vehiculeIndispensable: this.vehiculeIndispensable()!,
      assistance0km: this.assistance0km()!,
      protectionConducteur: this.protectionConducteur()!,
      brisGlace: this.brisGlace()!,
      accompagnementLitige: this.accompagnementLitige()!,
      priorite: this.priorite()!,
    };

    this.envoi.set(true);
    this.besoinsService.enregistrer(id, besoinsVersRequete(besoins)).subscribe({
      next: (rep) => {
        this.devisAutoService.enregistrerBesoinsEtRecommandation(besoins, reponseVersRecommandation(rep));
        this.envoi.set(false);
        this.router.navigate(['/devis-auto/formule']);
      },
      error: () => {
        this.envoi.set(false);
        this.erreur.set('Une erreur est survenue. Veuillez réessayer.');
      },
    });
  }

  protected formaterPrix(prix: number): string {
    return prix.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}


