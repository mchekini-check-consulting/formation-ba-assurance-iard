import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Composant racine : affiche l'écran correspondant à l'adresse (voir app.routes.ts). */
@Component({
  imports: [RouterOutlet],
  selector: 'iard-root',
  template: '<router-outlet />',
})
export class App {}
