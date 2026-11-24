import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CONTRAT_AUTO } from '../../core/model/contrat.exemples';
import { TableauDeBord } from '../../core/model/tableau-de-bord';
import { AnnonceService } from '../../core/service/annonce.service';
import { ConnexionService } from '../../core/service/connexion.service';
import { EspaceClient } from './espace-client';

const URL_TABLEAU = '/api/assurance-iard-api/espace-client/tableau-de-bord';
const DROITS_ASSURE = ['CONTRATS:CONSULTER', 'DEVIS:CONSULTER', 'DOCUMENTS:CONSULTER', 'SINISTRES:CONSULTER'];

const TABLEAU: TableauDeBord = {
  prenom: 'Camille',
  contrats: [CONTRAT_AUTO],
  devisVisibles: true,
  devisEnCours: [
    {
      produit: 'AUTO',
      description: 'Peugeot 208 · EF-003-GH',
      formule: 'TR',
      prixMensuel: 54.9,
      dateFinValidite: '2026-12-24',
    },
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
    {
      numero: 'SIN-1',
      numeroContrat: 'AE-RECETTE-A03',
      type: 'BRIS_GLACE',
      dateSurvenance: '2026-11-14',
      declareLe: null,
    },
  ],
};

describe('EspaceClient : tableau de bord (US-18)', () => {
  let fixture: ComponentFixture<EspaceClient>;
  let page: HTMLElement;
  let router: Router;
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

  beforeEach(() => {
    sessionStorage.setItem('assureasy.session', 'session');
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
    connexionService = TestBed.inject(ConnexionService);
    connexionService.emailClient.set('camille@exemple.fr');
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
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

    it('accueille le client par son prénom', () => {
      expect(page.querySelector('h1')?.textContent?.trim()).toBe('Bonjour Camille');
      expect(page.querySelector('main strong')?.textContent).toBe('camille@exemple.fr');
    });

    it('affiche ses contrats avec numéro, statut, prime et échéance', () => {
      const carte = page.querySelector('iard-carte-contrat');
      expect(carte?.textContent).toContain('AE-RECETTE-A03');
      expect(carte?.querySelector('.badge-statut')?.textContent?.trim()).toBe('Actif');
      expect(carte?.textContent).toContain('45,00');
      expect(carte?.textContent).toContain('Échéance : 25/09/2027');
    });

    it('affiche ses devis en cours, ses dernières factures et ses sinistres déclarés', () => {
      expect(page.textContent).toContain('Auto · Tous risques');
      expect(page.textContent).toContain('Peugeot 208 · EF-003-GH');
      expect(page.textContent).toContain('Valable jusqu\'au 24/12/2026');
      expect(page.textContent).toContain('Facture FAC-1');
      expect(page.textContent).toContain('Du 01/10/2026 au 31/10/2026');
      expect(page.textContent).toContain('PDF bientôt disponible.');
      expect(page.textContent).toContain('Bris de glace');
      expect(page.textContent).toContain('Sinistre SIN-1');
    });

    it('télécharge une facture, ou signale qu’elle est en préparation (409)', () => {
      const bouton = (): HTMLButtonElement =>
        Array.from(page.querySelectorAll<HTMLButtonElement>('main button')).find((b) => b.textContent?.includes('FAC-1'))!;

      bouton().click();
      http.expectOne('/api/assurance-iard-api/documents-contrat/50').flush(new Blob(), { status: 409, statusText: 'x' });
      fixture.detectChanges();
      expect(page.querySelector('[role="status"]')?.textContent).toBe(
        'Document en préparation, réessayez dans quelques minutes.',
      );

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
      Array.from(page.querySelectorAll<HTMLButtonElement>('main button'))
        .find((b) => b.textContent?.includes('FAC-1'))!
        .click();
      http.expectOne('/api/assurance-iard-api/documents-contrat/50').flush(new Blob(), { status: 500, statusText: 'x' });
      fixture.detectChanges();
      expect(page.querySelector('[role="status"]')?.textContent).toContain("n'a pas pu être téléchargé");
    });
  });

  it('AC6 : sans contrat, un message oriente vers la souscription habitation ou auto', () => {
    afficherAvec('PROSPECT', ['CONTRATS:CONSULTER', 'DEVIS:CONSULTER']);
    repondre({ ...TABLEAU, prenom: 'David', contrats: [], dernieresFactures: [], devisEnCours: [], sinistresVisibles: false, sinistres: [] });

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Bonjour David');
    expect(page.textContent).toContain("Vous n'avez pas encore de contrat.");
    const liens = Array.from(page.querySelectorAll('.sans-contrat a')).map((lien) => lien.getAttribute('href'));
    expect(liens).toEqual(['/devis/logement', '/devis-auto/vehicule']);
    expect(page.textContent).toContain('Aucun devis en cours.');
    expect(page.textContent).toContain('Aucune facture pour le moment.');
    expect(page.textContent).not.toContain('Mes sinistres déclarés');
  });

  it('une rubrique non autorisée (devis) n’est pas affichée', () => {
    afficherAvec('ASSURE', ['CONTRATS:CONSULTER']);
    repondre({ ...TABLEAU, devisVisibles: false, devisEnCours: [], sinistresVisibles: false, sinistres: [] });

    expect(page.textContent).not.toContain('Mes devis en cours');
    expect(page.textContent).not.toContain('Mes sinistres déclarés');
  });

  it("affiche un message clair si l'espace n'a pas pu être chargé", () => {
    afficherAvec('ASSURE', DROITS_ASSURE);
    repondre({}, 500);

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Mon espace');
    expect(page.querySelector('[role="alert"]')?.textContent).toContain("n'a pas pu être chargé");
  });

  describe('US-04, AC3 et US-18, RG5 : navigation selon les droits du profil, au-dessus du contenu', () => {
    const liens = (): string[] =>
      Array.from(page.querySelectorAll('nav a')).map((lien) => lien.textContent?.trim() ?? '');

    it("un assuré voit ses rubriques autorisées et jamais le back-office, même avec le droit « Comptes »", () => {
      afficherAvec('ASSURE', ['DEVIS:CONSULTER', 'CONTRATS:CONSULTER', 'COMPTES_UTILISATEURS:CONSULTER']);
      repondre(TABLEAU);

      expect(liens()).toEqual(['Mes contrats', 'Mes devis']);
    });

    it('une rubrique dont le droit est retiré disparaît, et le tableau de bord n’est pas demandé sans le droit Contrats', () => {
      afficherAvec('ASSURE', ['DEVIS:CONSULTER']);

      expect(liens()).toEqual(['Mes devis']);
      http.expectNone(URL_TABLEAU);
    });

    it("l'administrateur voit le back-office, la gestion des profils et le journal", () => {
      afficherAvec('ADMINISTRATEUR', ['COMPTES_UTILISATEURS:CONSULTER', 'PROFILS:CONSULTER', 'DOCUMENTS:CONSULTER']);

      expect(liens()).toEqual(['Mes documents', 'Dossiers clients', 'Gérer les profils', 'Journal des accès refusés']);
    });

    it('le gestionnaire voit les dossiers clients mais pas la gestion des profils', () => {
      afficherAvec('GESTIONNAIRE', ['COMPTES_UTILISATEURS:CONSULTER']);

      expect(liens()).toEqual(['Dossiers clients']);
    });
  });

  describe('US-07, AC3 : déconnexion', () => {
    beforeEach(() => afficherAvec('GESTIONNAIRE', []));

    it("propose « Se déconnecter » dans l'en-tête", () => {
      expect(page.querySelector('header button')?.textContent).toContain('Se déconnecter');
    });

    it("ferme la session, revient à l'accueil et confirme la déconnexion", () => {
      page.querySelector<HTMLButtonElement>('header button')!.click();
      const requete = http.expectOne('/api/assurance-iard-api/session');
      expect(requete.request.method).toBe('DELETE');
      requete.flush(null, { status: 204, statusText: 'No Content' });

      expect(connexionService.jeton()).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/']);
      expect(TestBed.inject(AnnonceService).prendre()).toBe('Vous êtes bien déconnecté.');
    });
  });
});
