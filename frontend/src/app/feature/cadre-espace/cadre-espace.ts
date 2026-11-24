import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { navigationClient, navigationInterne } from '../../core/config/raccourcis';
import { formaterNomPropre, initiales } from '../../core/model/identite';
import { AnnonceService } from '../../core/service/annonce.service';
import { ConnexionService } from '../../core/service/connexion.service';
import { EspaceClientService } from '../../core/service/espace-client.service';

/**
 * US-18 : cadre des pages de l'espace client. En-tête (logo ; initiales, prénom et nom, « Se déconnecter »),
 * barre latérale à gauche sur ordinateur, au-dessus du contenu sur mobile (RG5). Les liens du back-office sont dans
 * un bloc « Espace interne » séparé, pour les profils internes seulement (US-04).
 * Gabarit de route : parent de toutes les pages de /espace-client, /back-office et /admin (app.routes.ts). Une page
 * accessible depuis la barre latérale s'affiche toujours dans ce cadre (règle vérifiée par app.routes.spec.ts).
 */
@Component({
  selector: 'iard-cadre-espace',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './cadre-espace.html',
  styleUrl: './cadre-espace.scss',
})
export class CadreEspace {
  private readonly connexion = inject(ConnexionService);
  private readonly espaceClient = inject(EspaceClientService);
  private readonly annonce = inject(AnnonceService);
  private readonly router = inject(Router);

  protected readonly profil = this.connexion.profilClient;

  /** Adresse de la page affichée, sans paramètres (pour mettre en évidence le lien actif). */
  private readonly adresse = toSignal(
    this.router.events.pipe(
      filter((evenement) => evenement instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  /** Identité du compte connecté seulement (jamais celle d'une session précédente). */
  private readonly identite = computed(() => {
    const identite = this.espaceClient.identite();
    return identite && identite.email === this.connexion.emailClient() ? identite : null;
  });
  protected readonly nomAffiche = computed(() => {
    const identite = this.identite();
    const nom = [formaterNomPropre(identite?.prenom), formaterNomPropre(identite?.nom)].filter(Boolean).join(' ');
    return nom || (this.connexion.emailClient() ?? '');
  });
  protected readonly initiales = computed(() => {
    const identite = this.identite();
    return identite ? initiales(identite.prenom, identite.nom) : '';
  });

  protected readonly navigation = computed(() => {
    this.connexion.droitsClient();
    return navigationClient().filter((entree) => !entree.rubrique || this.peut(entree.rubrique));
  });

  /** Liens du back-office autorisés : profils internes seulement (le bloc n'est pas affiché sinon). */
  protected readonly navigationInterne = computed(() => {
    this.connexion.droitsClient();
    const profil = this.connexion.profilClient();
    return navigationInterne().filter((entree) =>
      entree.administrateur ? profil === 'ADMINISTRATEUR' : !entree.rubrique || this.peut(entree.rubrique),
    );
  });

  constructor() {
    // L'en-tête a besoin du prénom et du nom : lus avec le tableau de bord (un seul appel si la page le demande aussi).
    if (!this.identite() && this.peut('CONTRATS')) {
      this.espaceClient.tableauDeBord(this.connexion.emailClient()).subscribe();
    }
  }

  /** Lien actif : même adresse, ou une page en dessous (la fiche d'un contrat pour « Mes contrats »). */
  protected estActif(adresse: string, exacte = false): boolean {
    const courante = this.adresse().split(/[?#]/)[0];
    return courante === adresse || (!exacte && courante.startsWith(adresse + '/'));
  }

  protected peut(
    rubrique: 'CONTRATS' | 'DEVIS' | 'DOCUMENTS' | 'SINISTRES' | 'COMPTES_UTILISATEURS' | 'PROFILS',
  ): boolean {
    return this.connexion.aLeDroit(rubrique, 'CONSULTER');
  }

  protected estInterne(): boolean {
    return this.connexion.estInterne();
  }

  protected seDeconnecter(): void {
    this.connexion.deconnecter().subscribe(() => {
      this.espaceClient.identite.set(null);
      this.annonce.preparer('Vous êtes bien déconnecté.');
      void this.router.navigate(['/']);
    });
  }
}
