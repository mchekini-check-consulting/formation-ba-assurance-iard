import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CompteService } from '../../core/service/compte.service';

@Component({
  selector: 'iard-compte-cree',
  imports: [RouterLink],
  styleUrl: './compte-cree.scss',
  templateUrl: './compte-cree.html',
})
export class CompteCree {
  protected readonly email = inject(CompteService).emailConnecte;
  protected readonly annonce = signal<string | null>(null);
  protected readonly parametresRetour = inject(ActivatedRoute).snapshot.queryParams;

  protected faireMonDevis(): void {
    this.annonce.set('Le devis en ligne sera bientôt disponible.');
  }

  protected fermerAnnonce(): void {
    this.annonce.set(null);
  }
}
