import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { compteConnecteGuard } from './core/guards/compte-connecte.guard';
import { droitGuard } from './core/guards/droit.guard';
import { achatRenseigneGuard, conducteurRenseigneGuard, usageRenseigneGuard, vehiculeRenseigneGuard } from './core/guards/devis-auto.guard';
import {
  formuleChoisieGuard,
  informationsEnregistreesGuard,
  logementRenseigneGuard,
  mandatSigneGuard,
  recommandationEnregistreeGuard,
} from './core/guards/devis.guard';
import { sessionGuard, verificationGuard } from './core/guards/session.guard';
import { CompteCree } from './feature/compte-cree/compte-cree';
import { Connexion } from './feature/connexion/connexion';
import { DevisAutoAchat } from './feature/devis-auto-achat/devis-auto-achat';
import { DevisAutoConducteur } from './feature/devis-auto-conducteur/devis-auto-conducteur';
import { DevisAutoBesoins } from './feature/devis-auto-besoins/devis-auto-besoins';
import { DevisAutoUsage } from './feature/devis-auto-usage/devis-auto-usage';
import { DevisAutoVehicule } from './feature/devis-auto-vehicule/devis-auto-vehicule';
import { DevisBesoins } from './feature/devis-besoins/devis-besoins';
import { DevisFormule } from './feature/devis-formule/devis-formule';
import { DevisContrat } from './feature/devis-contrat/devis-contrat';
import { DevisInformations } from './feature/devis-informations/devis-informations';
import { DevisLogement } from './feature/devis-logement/devis-logement';
import { DevisPaiement } from './feature/devis-paiement/devis-paiement';
import { DoubleAuthentification } from './feature/double-authentification/double-authentification';
import { Inscription } from './feature/inscription/inscription';
import { Landing } from './feature/landing/landing';
import { GestionProfils } from './feature/gestion-profils/gestion-profils';
import { MesDevis } from './feature/mes-devis/mes-devis';
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
    // US-18 : tableau de bord chargé à la demande (comme les autres pages de l'espace client).
    loadComponent: () => import('./feature/espace-client/espace-client').then((m) => m.EspaceClient),
    canActivate: [sessionGuard],
    title: "Mon espace – Assur'Easy",
  },
  // US-04 : chaque page vérifie le droit du profil, relu auprès du serveur (le serveur refait le contrôle).
  {
    path: 'espace-client/documents',
    loadComponent: () => import('./feature/mes-documents/mes-documents').then((m) => m.MesDocuments),
    canActivate: [droitGuard('DOCUMENTS', 'CONSULTER')],
    title: "Mes documents – Assur'Easy",
  },
  {
    path: 'espace-client/documents/:id',
    loadComponent: () => import('./feature/document-client/document-client').then((m) => m.DocumentClientPage),
    canActivate: [droitGuard('DOCUMENTS', 'CONSULTER')],
    title: "Document – Assur'Easy",
  },
  {
    path: 'espace-client/devis',
    component: MesDevis,
    canActivate: [droitGuard('DEVIS', 'CONSULTER')],
    title: "Mes devis – Assur'Easy",
  },
  {
    path: 'espace-client/contrats',
    loadComponent: () => import('./feature/mes-contrats/mes-contrats').then((m) => m.MesContrats),
    canActivate: [droitGuard('CONTRATS', 'CONSULTER')],
    title: "Mes contrats – Assur'Easy",
  },
  {
    // US-18 : fiche par numéro ; une ancienne adresse par référence de devis redirige vers le numéro.
    path: 'espace-client/contrats/:numero',
    loadComponent: () => import('./feature/contrat-client/contrat-client').then((m) => m.ContratClient),
    canActivate: [droitGuard('CONTRATS', 'CONSULTER')],
    title: "Mon contrat – Assur'Easy",
  },
  {
    path: 'back-office/clients',
    loadComponent: () => import('./feature/dossiers-clients/dossiers-clients').then((m) => m.DossiersClients),
    canActivate: [droitGuard('COMPTES_UTILISATEURS', 'CONSULTER', true)],
    title: "Dossiers clients – Assur'Easy",
  },
  {
    path: 'back-office/clients/:id',
    loadComponent: () => import('./feature/dossier-client/dossier-client').then((m) => m.DossierClient),
    canActivate: [droitGuard('COMPTES_UTILISATEURS', 'CONSULTER', true)],
    title: "Dossier client – Assur'Easy",
  },
  {
    path: 'admin/acces-refuses',
    loadComponent: () => import('./feature/journal-acces/journal-acces').then((m) => m.JournalAcces),
    canActivate: [droitGuard('PROFILS', 'CONSULTER', true)],
    title: "Journal des accès refusés – Assur'Easy",
  },
  {
    path: 'acces-non-autorise',
    loadComponent: () => import('./feature/acces-non-autorise/acces-non-autorise').then((m) => m.AccesNonAutorise),
    title: "Accès non autorisé – Assur'Easy",
  },
  { path: 'devis-auto/vehicule', component: DevisAutoVehicule, title: "Étape 1.1 – Retrouver mon véhicule – Assur'Easy" },
  {
    path: 'devis-auto/achat',
    component: DevisAutoAchat,
    canActivate: [vehiculeRenseigneGuard],
    title: "Étape 1.2 – Achat et carte grise – Assur'Easy",
  },
  {
    path: 'devis-auto/usage',
    component: DevisAutoUsage,
    canActivate: [achatRenseigneGuard],
    title: "Étape 1.3 – Usage et stationnement – Assur'Easy",
  },
  {
    path: 'devis-auto/conducteur',
    component: DevisAutoConducteur,
    canActivate: [usageRenseigneGuard],
    title: "Étape 1.4 – Le conducteur principal – Assur'Easy",
  },
  {
    path: 'devis-auto/besoins',
    component: DevisAutoBesoins,
    canActivate: [conducteurRenseigneGuard],
    title: "Étape 2 – Mes besoins – Assur'Easy",
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
  {
    path: 'devis/informations',
    component: DevisInformations,
    canActivate: [formuleChoisieGuard, sessionGuard],
    title: "Mes informations – Assur'Easy",
  },
  {
    path: 'devis/paiement',
    component: DevisPaiement,
    canActivate: [informationsEnregistreesGuard, sessionGuard],
    title: "Paiement – Assur'Easy",
  },
  {
    path: 'devis/contrat',
    component: DevisContrat,
    canActivate: [mandatSigneGuard, sessionGuard],
    title: "Mon contrat – Assur'Easy",
  },
  {
    path: 'admin/profils',
    component: GestionProfils,
    canActivate: [adminGuard],
    title: "Gestion des profils – Assur'Easy",
  },
  { path: '**', redirectTo: '' },
];
