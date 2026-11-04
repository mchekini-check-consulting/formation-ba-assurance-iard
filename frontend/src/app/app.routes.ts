import { Routes } from '@angular/router';
import { compteConnecteGuard } from './core/guards/compte-connecte.guard';
import { CompteCree } from './feature/compte-cree/compte-cree';
import { DevisBesoins } from './feature/devis-besoins/devis-besoins';
import { DevisLogement } from './feature/devis-logement/devis-logement';
import { Inscription } from './feature/inscription/inscription';
import { Landing } from './feature/landing/landing';
import { PageSimple } from './feature/page-simple/page-simple';

export const routes: Routes = [
  { path: '', component: Landing, title: "Assur'Easy – Assurance habitation en ligne" },
  { path: 'cookies', component: PageSimple, title: "Cookies – Assur'Easy", data: { titre: 'Cookies' } },
  { path: 'cgu', component: PageSimple, title: "CGU – Assur'Easy", data: { titre: 'CGU' } },
  {
    path: 'accessibilite',
    component: PageSimple,
    title: "Accessibilité – Assur'Easy",
    data: { titre: 'Accessibilité' },
  },
  { path: 'contact', component: PageSimple, title: "Contact – Assur'Easy", data: { titre: 'Contact' } },
  { path: 'inscription', component: Inscription, title: "Créer mon compte – Assur'Easy" },
  {
    path: 'compte-cree',
    component: CompteCree,
    canActivate: [compteConnecteGuard],
    title: "Compte créé – Assur'Easy",
  },
  { path: 'devis/logement', component: DevisLogement, title: "Votre logement – Assur'Easy" },
  { path: 'devis/besoins', component: DevisBesoins, title: "Vos besoins – Assur'Easy" },
  { path: '**', redirectTo: '' },
];
