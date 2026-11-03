import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HelloService } from './core/service/hello.service';

@Component({
  imports: [RouterOutlet],
  selector: 'iard-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {
  private readonly helloService = inject(HelloService);

  protected readonly title = signal('Assurance IARD');
  // Message du backend (GET /hello), null tant qu'il n'est pas arrivé
  protected readonly message = signal<string | null>(null);
  protected readonly erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.helloService.direBonjour().subscribe({
      next: (message) => this.message.set(message),
      error: () => this.erreur.set('Le serveur ne répond pas, réessayez plus tard.'),
    });
  }
}
