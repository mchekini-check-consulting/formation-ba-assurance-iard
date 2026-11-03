import { Routes } from '@angular/router';
import { compteConnecteGuard } from './core/guards/compte-connecte.guard';
import { CompteCree } from './feature/compte-cree/compte-cree';
import { Inscription } from './feature/inscription/inscription';
import { Landing } from './feature/landing/landing';

export const routes: Routes = [
  { path: '', component: Landing, title: "Assur'Easy – Assurance habitation en ligne" },
  { path: 'inscription', component: Inscription, title: "Créer mon compte – Assur'Easy" },
  {
    path: 'compte-cree',
    component: CompteCree,
    canActivate: [compteConnecteGuard],
    title: "Compte créé – Assur'Easy",
  },
  { path: '**', redirectTo: '' },
];
