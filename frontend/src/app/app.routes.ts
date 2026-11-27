import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { compteConnecteGuard } from './core/guards/compte-connecte.guard';
import { droitGuard } from './core/guards/droit.guard';
import { achatRenseigneGuard, besoinsRenseignesGuard, conducteurRenseigneGuard, contratAutoGenereGuard, formuleChoisieAutoGuard, souscripteurEnregistreAutoGuard, usageRenseigneGuard, vehiculeRenseigneGuard } from './core/guards/devis-auto.guard';
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
import { DevisAutoFormule } from './feature/devis-auto-formule/devis-auto-formule';
import { DevisAutoInformations } from './feature/devis-auto-informations/devis-auto-informations';
import { DevisAutoPaiement } from './feature/devis-auto-paiement/devis-auto-paiement';
import { DevisAutoContratStub } from './feature/devis-auto-contrat-stub/devis-auto-contrat-stub';
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
import { PageSimple } from './feature/page-simple/page-simple';
import { ReinitialisationMotDePasse } from './feature/reinitialisation-mot-de-passe/reinitialisation-mot-de-passe';

/** US-18 : le cadre commun de l'espace (en-tête et barre latérale), chargé à la demande. */
export const CADRE_ESPACE = () => import('./feature/cadre-espace/cadre-espace').then((m) => m.CadreEspace);

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
  // US-18 : gabarit de route. Toute page de l'espace client, du back-office et de l'administration s'affiche dans
  // le cadre commun (en-tête, barre latérale à gauche) : ajouter une page = ajouter une route enfant ici.
  // US-04 : chaque page garde sa garde de droit, relue auprès du serveur (le serveur refait le contrôle).
  {
    path: 'espace-client',
    loadComponent: CADRE_ESPACE,
    children: [
      {
        path: '',
        loadComponent: () => import('./feature/espace-client/espace-client').then((m) => m.EspaceClient),
        canActivate: [sessionGuard],
        title: "Mon espace – Assur'Easy",
      },
      {
        // US-22 : échéancier et factures (génération : droit DOCUMENTS/CREER, vérifié par le serveur).
        path: 'factures',
        loadComponent: () => import('./feature/mes-factures/mes-factures').then((m) => m.MesFactures),
        canActivate: [droitGuard('CONTRATS', 'CONSULTER')],
        title: "Mes factures – Assur'Easy",
      },
      {
        // US-24 : dossiers déclarés et brouillons (AC8).
        path: 'sinistres',
        loadComponent: () => import('./feature/mes-sinistres/mes-sinistres').then((m) => m.MesSinistres),
        canActivate: [droitGuard('SINISTRES', 'CONSULTER')],
        title: "Mes sinistres – Assur'Easy",
      },
      {
        // US-24, étape 1 : contrat et type (AC1, AC2) ; ?contrat=<numéro> depuis le raccourci d'un contrat.
        path: 'sinistres/declarer',
        loadComponent: () => import('./feature/declarer-sinistre/declarer-sinistre').then((m) => m.DeclarerSinistre),
        canActivate: [droitGuard('SINISTRES', 'CREER')],
        title: "Déclarer un sinistre – Assur'Easy",
      },
      {
        // US-24, étape 1 d'un brouillon (AC8).
        path: 'sinistres/:id/type',
        loadComponent: () => import('./feature/declarer-sinistre/declarer-sinistre').then((m) => m.DeclarerSinistre),
        canActivate: [droitGuard('SINISTRES', 'CREER')],
        title: "Déclarer un sinistre – Assur'Easy",
      },
      {
        // US-24, étape 2 (AC4 à AC6, AC11 à AC13).
        path: 'sinistres/:id/circonstances',
        loadComponent: () =>
          import('./feature/sinistre-circonstances/sinistre-circonstances').then((m) => m.SinistreCirconstances),
        canActivate: [droitGuard('SINISTRES', 'CREER')],
        title: "Circonstances du sinistre – Assur'Easy",
      },
      {
        // US-24, étape 3 (AC7, AC9, AC14 à AC16) ; dossier envoyé : compléter ses justificatifs.
        path: 'sinistres/:id/justificatifs',
        loadComponent: () =>
          import('./feature/sinistre-justificatifs/sinistre-justificatifs').then((m) => m.SinistreJustificatifs),
        canActivate: [droitGuard('SINISTRES', 'CREER')],
        title: "Justificatifs du sinistre – Assur'Easy",
      },
      {
        // US-24, AC9 : « Déclaration envoyée ».
        path: 'sinistres/:id/envoyee',
        loadComponent: () => import('./feature/sinistre-envoye/sinistre-envoye').then((m) => m.SinistreEnvoyePage),
        canActivate: [droitGuard('SINISTRES', 'CONSULTER')],
        title: "Déclaration envoyée – Assur'Easy",
      },
      {
        path: 'documents',
        loadComponent: () => import('./feature/mes-documents/mes-documents').then((m) => m.MesDocuments),
        canActivate: [droitGuard('DOCUMENTS', 'CONSULTER')],
        title: "Mes documents – Assur'Easy",
      },
      {
        path: 'documents/:id',
        loadComponent: () => import('./feature/document-client/document-client').then((m) => m.DocumentClientPage),
        canActivate: [droitGuard('DOCUMENTS', 'CONSULTER')],
        title: "Document – Assur'Easy",
      },
      {
        path: 'devis',
        loadComponent: () => import('./feature/mes-devis/mes-devis').then((m) => m.MesDevis),
        canActivate: [droitGuard('DEVIS', 'CONSULTER')],
        title: "Mes devis – Assur'Easy",
      },
      {
        path: 'contrats',
        loadComponent: () => import('./feature/mes-contrats/mes-contrats').then((m) => m.MesContrats),
        canActivate: [droitGuard('CONTRATS', 'CONSULTER')],
        title: "Mes contrats – Assur'Easy",
      },
      {
        // US-18 : fiche par numéro ; une ancienne adresse par référence de devis redirige vers le numéro.
        path: 'contrats/:numero',
        loadComponent: () => import('./feature/contrat-client/contrat-client').then((m) => m.ContratClient),
        canActivate: [droitGuard('CONTRATS', 'CONSULTER')],
        title: "Mon contrat – Assur'Easy",
      },
      {
        // US-23, AC1-AC3 : garanties du contrat (le serveur refuse un contrat qui n'est pas actif, AC6).
        path: 'contrats/:numero/garanties',
        loadComponent: () =>
          import('./feature/modifier-garanties/modifier-garanties').then((m) => m.ModifierGaranties),
        canActivate: [droitGuard('CONTRATS', 'MODIFIER')],
        title: "Modifier mes garanties – Assur'Easy",
      },
      {
        // US-23, AC3-AC5 : relire, accepter ou abandonner l'avenant en attente.
        path: 'contrats/:numero/avenant',
        loadComponent: () => import('./feature/relire-avenant/relire-avenant').then((m) => m.RelireAvenant),
        canActivate: [droitGuard('CONTRATS', 'MODIFIER')],
        title: "Relire et accepter l'avenant – Assur'Easy",
      },
      {
        // US-23, AC4 : confirmation de l'avenant accepté.
        path: 'contrats/:numero/avenant-accepte',
        loadComponent: () => import('./feature/avenant-accepte/avenant-accepte').then((m) => m.AvenantAccepte),
        canActivate: [droitGuard('CONTRATS', 'CONSULTER')],
        title: "Avenant accepté – Assur'Easy",
      },
    ],
  },
  {
    path: 'back-office',
    loadComponent: CADRE_ESPACE,
    children: [
      {
        path: 'clients',
        loadComponent: () => import('./feature/dossiers-clients/dossiers-clients').then((m) => m.DossiersClients),
        canActivate: [droitGuard('COMPTES_UTILISATEURS', 'CONSULTER', true)],
        title: "Dossiers clients – Assur'Easy",
      },
      {
        path: 'clients/:id',
        loadComponent: () => import('./feature/dossier-client/dossier-client').then((m) => m.DossierClient),
        canActivate: [droitGuard('COMPTES_UTILISATEURS', 'CONSULTER', true)],
        title: "Dossier client – Assur'Easy",
      },
    ],
  },
  {
    path: 'admin',
    loadComponent: CADRE_ESPACE,
    children: [
      {
        path: 'profils',
        loadComponent: () => import('./feature/gestion-profils/gestion-profils').then((m) => m.GestionProfils),
        canActivate: [adminGuard],
        title: "Gestion des profils – Assur'Easy",
      },
      {
        path: 'acces-refuses',
        loadComponent: () => import('./feature/journal-acces/journal-acces').then((m) => m.JournalAcces),
        canActivate: [droitGuard('PROFILS', 'CONSULTER', true)],
        title: "Journal des accès refusés – Assur'Easy",
      },
    ],
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
  {
    path: 'devis-auto/formule',
    component: DevisAutoFormule,
    canActivate: [besoinsRenseignesGuard],
    title: "Étape 3 – Ma formule – Assur'Easy",
  },
  {
    path: 'devis-auto/informations',
    component: DevisAutoInformations,
    canActivate: [formuleChoisieAutoGuard, sessionGuard],
    title: "Étape 4 – Mes informations – Assur'Easy",
  },
  {
    path: 'devis-auto/paiement',
    component: DevisAutoPaiement,
    canActivate: [souscripteurEnregistreAutoGuard, sessionGuard],
    title: "Étape 5 – Paiement – Assur'Easy",
  },
  {
    path: 'devis-auto/contrat',
    component: DevisAutoContratStub,
    canActivate: [contratAutoGenereGuard, sessionGuard],
    title: "Étape 6 – Mon contrat – Assur'Easy",
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
  { path: '**', redirectTo: '' },
];
