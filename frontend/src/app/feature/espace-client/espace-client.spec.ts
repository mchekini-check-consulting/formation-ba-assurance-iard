import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContratResume, StatutContrat } from '../../core/model/contrat';
import { CONTRAT_AUTO, CONTRAT_HABITATION } from '../../core/model/contrat.exemples';
import { TableauDeBord } from '../../core/model/tableau-de-bord';
import { ConnexionService } from '../../core/service/connexion.service';
import { EspaceClient } from './espace-client';

const URL_TABLEAU = '/api/assurance-iard-api/espace-client/tableau-de-bord';
const DROITS_ASSURE = ['CONTRATS:CONSULTER', 'DEVIS:CONSULTER', 'DOCUMENTS:CONSULTER', 'SINISTRES:CONSULTER'];

const TABLEAU: TableauDeBord = {
  prenom: 'Camille',
  nom: 'Durand',
  contrats: [CONTRAT_AUTO, CONTRAT_HABITATION],
  devisVisibles: true,
  devisEnCours: [
    { produit: 'AUTO', description: 'Peugeot 208 · EF-004-GH', formule: 'TR', prixMensuel: 54.9, dateFinValidite: '2026-12-24' },
    { produit: 'HABITATION', description: '3 rue des Devis', formule: 'confort', prixMensuel: 12, dateFinValidite: '2026-12-01' },
  ],
  dernieresFactures: [
    {
      numero: 'FAC-1',
      numeroContrat: 'AE-RECETTE-A03',
      periodeDebut: '2026-10-01',
      periodeFin: '2026-10-31',
      montantTtc: 45,
      documentId: 50,
    },
    {
      numero: 'FAC-2',
      numeroContrat: 'AE-RECETTE-A03',
      periodeDebut: '2026-09-01',
      periodeFin: '2026-09-30',
      montantTtc: 45,
      documentId: null,
    },
  ],
  sinistresVisibles: true,
  sinistres: [
    { numero: 'SIN-1', numeroContrat: 'AE-RECETTE-A03', type: 'BRIS_GLACE', dateSurvenance: '2026-11-14', declareLe: null },
  ],
};

describe('EspaceClient : tableau de bord (US-18, maquette)', () => {
  let fixture: ComponentFixture<EspaceClient>;
  let page: HTMLElement;
  let http: HttpTestingController;
  let connexionService: ConnexionService;

  const afficherAvec = (profil: string, droits: string[]): void => {
    connexionService.profilClient.set(profil);
    connexionService.droitsClient.set(droits);
    fixture = TestBed.createComponent(EspaceClient);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };
  const repondre = (corps: object, status = 200): void => {
    http.expectOne(URL_TABLEAU).flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };
  const texte = (selecteur: string): string =>
    (page.querySelector(selecteur)?.textContent ?? '').replace(/\s+/g, ' ').trim();
  const bloc = (titre: string): HTMLElement | null =>
    Array.from(page.querySelectorAll<HTMLElement>('.bloc')).find((b) => b.querySelector('h2')?.textContent === titre) ??
    null;

  beforeEach(() => {
    sessionStorage.setItem('assureasy.session', 'session');
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    connexionService = TestBed.inject(ConnexionService);
    connexionService.emailClient.set('camille@exemple.fr');
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  describe('AC1 : l’essentiel de mes assurances', () => {
    beforeEach(() => {
      afficherAvec('ASSURE', DROITS_ASSURE);
      expect(page.textContent).toContain('Chargement de votre espace');
      repondre(TABLEAU);
    });

    it('accueille le client par son prénom, avec le sous-titre de la maquette', () => {
      expect(texte('h1')).toBe('Bonjour Camille');
      expect(texte('.tableau-de-bord__sous-titre')).toBe("L'essentiel de vos assurances, en un coup d'œil.");
      // En-tête (initiales, nom) : fourni par le cadre commun, testé dans cadre-espace.spec.ts.
      expect(page.querySelector('header')).toBeNull();
    });

    it('« Mes contrats » : lien « Tout voir » et cartes en grille', () => {
      const tout = page.querySelector<HTMLAnchorElement>('.section__entete a');
      expect(tout?.textContent?.trim()).toBe('Tout voir');
      expect(tout?.getAttribute('href')).toBe('/espace-client/contrats');
      expect(page.querySelectorAll('.grille--contrats iard-carte-contrat')).toHaveLength(2);
    });

    it('« Prochaine échéance » : date, produit, montant, IBAN masqué et dernières factures', () => {
      const echeance = bloc('Prochaine échéance')!;
      const contenu = echeance.textContent?.replace(/\s+/g, ' ') ?? '';
      expect(contenu).toContain('25/09/2027');
      expect(contenu).toContain('Auto · N° AE-RECETTE-A03');
      expect(contenu).toContain('45,00 €/mois TTC');
      expect(echeance.querySelector('.iban')?.textContent).toBe('•••• 7788');
      expect(contenu).toContain('FAC-1 · 45,00 €');
      expect(contenu).toContain('PDF bientôt disponible');
      // US-22 livrée : lien « Voir mes factures ».
      const lien = Array.from(echeance.querySelectorAll('a')).find((a) => a.textContent?.includes('Voir mes factures'));
      expect(lien?.getAttribute('href')).toBe('/espace-client/factures');
    });

    it('« Devis en cours » : le plus récent, sans « Voir mon devis » pour un devis auto (AC4 auto reporté)', () => {
      const devis = bloc('Devis en cours')!;
      const contenu = devis.textContent?.replace(/\s+/g, ' ') ?? '';
      expect(contenu).toContain('Auto · Peugeot 208 · EF-004-GH');
      expect(contenu).toContain('54,90 €/mois TTC');
      expect(contenu).toContain("Valable jusqu'au 24/12/2026");
      expect(devis.querySelector('a')).toBeNull();
    });

    it('« Sinistres en cours » : type, produit et statut en texte, sans lien tant que US-24 n’est pas livrée', () => {
      const sinistres = bloc('Sinistres en cours')!;
      const contenu = sinistres.textContent?.replace(/\s+/g, ' ') ?? '';
      expect(contenu).toContain('Bris de glace');
      expect(contenu).toContain('Auto · Sinistre SIN-1');
      expect(sinistres.querySelector('.badge-sinistre')?.textContent).toBe('Déclaré');
      expect(contenu).not.toContain('Voir mes sinistres');
    });

    it('« Que voulez-vous faire ? » : tuiles « Modifier mes garanties » (US-23), « Mes factures » (US-22) et « Faire un nouveau devis »', () => {
      const tuiles = Array.from(page.querySelectorAll('.tuile'));
      expect(tuiles.map((t) => t.textContent?.trim())).toEqual([
        'Modifier mes garanties',
        'Mes factures',
        'Faire un nouveau devis',
      ]);
      // US-23 : choix du contrat dans « Mes contrats ».
      expect(tuiles[0].getAttribute('href')).toBe('/espace-client/contrats');
      expect(tuiles[1].getAttribute('href')).toBe('/espace-client/factures');
      // Page d'accueil, qui propose habitation et auto (US-26).
      expect(tuiles[2].getAttribute('href')).toBe('/');
    });

    it('télécharge une facture, ou signale qu’elle est en préparation (409)', () => {
      const bouton = (): HTMLButtonElement => page.querySelector<HTMLButtonElement>('.factures button')!;

      bouton().click();
      http.expectOne('/api/assurance-iard-api/documents-contrat/50').flush(new Blob(), { status: 409, statusText: 'x' });
      fixture.detectChanges();
      expect(texte('[role="status"]')).toBe('Document en préparation, réessayez dans quelques minutes.');

      vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:facture');
      vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
      const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
      bouton().click();
      http.expectOne('/api/assurance-iard-api/documents-contrat/50').flush(new Blob(['%PDF']));
      fixture.detectChanges();
      expect(clic).toHaveBeenCalled();
      expect(page.querySelector('[role="status"]')).toBeNull();
      vi.restoreAllMocks();
    });

    it('signale une autre erreur de téléchargement', () => {
      page.querySelector<HTMLButtonElement>('.factures button')!.click();
      http.expectOne('/api/assurance-iard-api/documents-contrat/50').flush(new Blob(), { status: 500, statusText: 'x' });
      fixture.detectChanges();
      expect(texte('[role="status"]')).toContain("n'a pas pu être téléchargé");
    });
  });

  it('4 contrats au plus : ceux qui demandent une action d’abord, puis les autres du plus récent au plus ancien', () => {
    afficherAvec('ASSURE', DROITS_ASSURE);
    const contrat = (numero: string, statut: StatutContrat): ContratResume => ({ ...CONTRAT_AUTO, numero, statut });
    // Ordre du serveur : du plus récent au plus ancien.
    repondre({
      ...TABLEAU,
      contrats: [
        contrat('AE-R1', 'ACTIF'),
        contrat('AE-R2', 'RESILIE'),
        contrat('AE-R3', 'IMPAYE'),
        contrat('AE-R4', 'EN_ATTENTE_EFFET'),
        contrat('AE-R5', 'EN_ATTENTE_DE_SIGNATURE'),
        contrat('AE-R6', 'ACTIF'),
      ],
    });

    const numeros = Array.from(page.querySelectorAll('.grille--contrats .carte__detail')).map(
      (detail) => detail.textContent?.match(/AE-R\d/)?.[0],
    );
    expect(numeros).toEqual(['AE-R3', 'AE-R5', 'AE-R1', 'AE-R2']);
    expect(page.querySelector('.section__entete a')?.getAttribute('href')).toBe('/espace-client/contrats');
  });

  it('les 4 statuts à traiter passent devant, même plus anciens', () => {
    afficherAvec('ASSURE', DROITS_ASSURE);
    const contrat = (numero: string, statut: StatutContrat): ContratResume => ({ ...CONTRAT_AUTO, numero, statut });
    repondre({
      ...TABLEAU,
      contrats: [
        contrat('AE-R1', 'ACTIF'),
        contrat('AE-R2', 'SUSPENDU'),
        contrat('AE-R3', 'AVENANT_EN_ATTENTE'),
        contrat('AE-R4', 'IMPAYE'),
        contrat('AE-R5', 'EN_ATTENTE_DE_SIGNATURE'),
      ],
    });

    const numeros = Array.from(page.querySelectorAll('.grille--contrats .carte__detail')).map(
      (detail) => detail.textContent?.match(/AE-R\d/)?.[0],
    );
    expect(numeros).toEqual(['AE-R2', 'AE-R3', 'AE-R4', 'AE-R5']);
  });

  it('le prénom saisi en majuscules s’affiche avec une majuscule initiale', () => {
    afficherAvec('ASSURE', DROITS_ASSURE);
    repondre({ ...TABLEAU, prenom: 'INES' });
    expect(texte('h1')).toBe('Bonjour Ines');
  });

  it('« Voir mon devis » mène à Mes devis pour un devis habitation', () => {
    afficherAvec('ASSURE', DROITS_ASSURE);
    repondre({ ...TABLEAU, devisEnCours: [TABLEAU.devisEnCours[1]] });
    expect(bloc('Devis en cours')?.querySelector('a')?.getAttribute('href')).toBe('/espace-client/devis');
  });

  it('cartes vides : message court', () => {
    afficherAvec('ASSURE', DROITS_ASSURE);
    repondre({
      ...TABLEAU,
      contrats: [{ ...CONTRAT_AUTO, dateEcheance: null }],
      devisEnCours: [],
      dernieresFactures: [],
      sinistres: [],
    });

    expect(bloc('Prochaine échéance')?.textContent).toContain('Aucune échéance à venir.');
    expect(bloc('Prochaine échéance')?.textContent).toContain('Aucune facture pour le moment.');
    expect(bloc('Devis en cours')?.textContent).toContain('Aucun devis en cours.');
    expect(bloc('Sinistres en cours')?.textContent).toContain('Aucun sinistre en cours.');
  });

  it('AC6 : sans contrat, un message oriente vers la souscription habitation ou auto', () => {
    afficherAvec('PROSPECT', ['CONTRATS:CONSULTER', 'DEVIS:CONSULTER']);
    repondre({
      ...TABLEAU,
      prenom: 'David',
      contrats: [],
      dernieresFactures: [],
      devisEnCours: [],
      sinistresVisibles: false,
      sinistres: [],
    });

    expect(texte('h1')).toBe('Bonjour David');
    expect(page.textContent).toContain("Vous n'avez pas encore de contrat.");
    const liens = Array.from(page.querySelectorAll('.sans-contrat a')).map((lien) => lien.getAttribute('href'));
    expect(liens).toEqual(['/devis/logement', '/devis-auto/vehicule']);
    expect(page.querySelector('.section__entete a')).toBeNull();
    // Sans contrat ni droit Sinistres : ces cartes disparaissent.
    expect(bloc('Prochaine échéance')).toBeNull();
    expect(bloc('Sinistres en cours')).toBeNull();
    expect(bloc('Devis en cours')?.textContent).toContain('Aucun devis en cours.');
  });

  it('une rubrique non autorisée (devis, sinistres) n’est pas affichée', () => {
    afficherAvec('ASSURE', ['CONTRATS:CONSULTER']);
    repondre({ ...TABLEAU, devisVisibles: false, devisEnCours: [], sinistresVisibles: false, sinistres: [] });

    expect(bloc('Devis en cours')).toBeNull();
    expect(bloc('Sinistres en cours')).toBeNull();
    expect(bloc('Prochaine échéance')).not.toBeNull();
  });

  it("affiche un message clair si l'espace n'a pas pu être chargé", () => {
    afficherAvec('ASSURE', DROITS_ASSURE);
    repondre({}, 500);

    expect(texte('h1')).toBe('Mon espace');
    expect(texte('[role="alert"]')).toContain("n'a pas pu être chargé");
  });

  it('sans le droit Contrats, le tableau de bord n’est pas demandé', () => {
    afficherAvec('GESTIONNAIRE', ['COMPTES_UTILISATEURS:CONSULTER']);

    http.expectNone(URL_TABLEAU);
    expect(texte('h1')).toBe('Mon espace');
  });
});
