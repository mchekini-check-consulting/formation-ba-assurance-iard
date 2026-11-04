import { Routes } from '@angular/router';
import { compteConnecteGuard } from './core/guards/compte-connecte.guard';
import { logementRenseigneGuard, recommandationEnregistreeGuard } from './core/guards/devis.guard';
import { sessionGuard, verificationGuard } from './core/guards/session.guard';
import { CompteCree } from './feature/compte-cree/compte-cree';
import { Connexion } from './feature/connexion/connexion';
import { DevisBesoins } from './feature/devis-besoins/devis-besoins';
import { DevisFormule } from './feature/devis-formule/devis-formule';
import { DevisLogement } from './feature/devis-logement/devis-logement';
import { DoubleAuthentification } from './feature/double-authentification/double-authentification';
import { EspaceClient } from './feature/espace-client/espace-client';
import { Inscription } from './feature/inscription/inscription';
import { Landing } from './feature/landing/landing';
import { PageSimple } from './feature/page-simple/page-simple';
import { ReinitialisationMotDePasse } from './feature/reinitialisation-mot-de-passe/reinitialisation-mot-de-passe';

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
  { path: 'connexion', component: Connexion, title: "Connexion – Assur'Easy" },
  {
    path: 'reinitialisation-mot-de-passe',
    component: ReinitialisationMotDePasse,
    title: "Nouveau mot de passe – Assur'Easy",
  },
  {
    path: 'double-authentification',
    component: DoubleAuthentification,
    canActivate: [verificationGuard],
    title: "Double authentification – Assur'Easy",
  },
  {
    path: 'espace-client',
    component: EspaceClient,
    canActivate: [sessionGuard],
    title: "Mon espace – Assur'Easy",
  },
  {
    path: 'espace-client/documents',
    component: PageSimple,
    canActivate: [sessionGuard],
    title: "Mes documents – Assur'Easy",
    data: { titre: 'Mes documents' },
  },
  { path: 'devis/logement', component: DevisLogement, title: "Votre logement – Assur'Easy" },
  {
    path: 'devis/besoins',
    component: DevisBesoins,
    canActivate: [logementRenseigneGuard],
    title: "Mes besoins – Assur'Easy",
  },
  {
    path: 'devis/formule',
    component: DevisFormule,
    canActivate: [recommandationEnregistreeGuard],
    title: "Ma formule – Assur'Easy",
  },
  { path: '**', redirectTo: '' },
];
