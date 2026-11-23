import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** US-04, AC2 : refus d'accès. Aucune donnée de la ressource demandée n'est affichée. */
@Component({
  selector: 'iard-acces-non-autorise',
  imports: [RouterLink],
  styleUrl: '../compte-cree/compte-cree.scss',
  templateUrl: './acces-non-autorise.html',
})
export class AccesNonAutorise {}
