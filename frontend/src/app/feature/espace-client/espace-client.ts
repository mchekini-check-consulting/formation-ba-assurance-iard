import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ConnexionService } from '../../core/service/connexion.service';

/** Tableau de bord du client connecté : contenu à venir. */
@Component({
  selector: 'iard-espace-client',
  imports: [RouterLink],
  styleUrl: '../compte-cree/compte-cree.scss',
  templateUrl: './espace-client.html',
})
export class EspaceClient {
  private readonly connexionService = inject(ConnexionService);
  private readonly router = inject(Router);

  protected readonly email = this.connexionService.emailClient;

  protected seDeconnecter(): void {
    this.connexionService.deconnecter().subscribe(() => void this.router.navigate(['/']));
  }
}
