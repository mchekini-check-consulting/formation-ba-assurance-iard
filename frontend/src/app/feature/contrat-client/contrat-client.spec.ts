import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { FICHE_AUTO } from '../../core/model/contrat.exemples';
import { ContratClient } from './contrat-client';

const REFERENCE = '7c9e6679-7425-40de-944b-e07fc1f90ae7';

describe('ContratClient (US-18, AC3)', () => {
  let fixture: ComponentFixture<ContratClient>;
  let page: HTMLElement;
  let http: HttpTestingController;
  let router: Router;
  let parametres: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  const ouvrir = (identifiant: string): void => {
    parametres = new BehaviorSubject(convertToParamMap({ numero: identifiant }));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: parametres } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(ContratClient);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };

  const repondreFiche = (numero: string, corps: object, status = 200): void => {
    http.expectOne(`/api/assurance-iard-api/contrats/${numero}`).flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };

  afterEach(() => http.verify());

  it('affiche la fiche auto : statut, véhicule, garanties, IBAN masqué et documents', () => {
    ouvrir('AE-RECETTE-A03');
    expect(page.textContent).toContain('Chargement du contrat');
    repondreFiche('AE-RECETTE-A03', FICHE_AUTO);

    expect(page.querySelector('h1')?.textContent).toContain('Contrat Auto AE-RECETTE-A03');
    expect(page.querySelector('.badge-statut')?.textContent?.trim()).toBe('Actif');
    expect(page.textContent).toContain('Véhicule assuré');
    expect(page.textContent).toContain('Renault Clio V TCe 90');
    expect(page.textContent).toContain('AB-003-CD');
    expect(page.textContent).toContain('Tous risques');
    expect(page.querySelector('.fiche__garanties li')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'Responsabilité civile (12,00 €/mois)',
    );
    expect(page.querySelector('.iban')?.textContent).toBe('•••• 7788');
    const documents = Array.from(page.querySelectorAll('.fiche__documents li span')).map((d) => d.textContent);
    expect(documents).toEqual(['Conditions particulières (version 1)', 'Conditions générales (version 1)']);
  });

  it('affiche la fiche habitation et le raccourci « Signer » si le contrat attend la signature', () => {
    ouvrir('AE-2026-000001');
    repondreFiche('AE-2026-000001', {
      ...FICHE_AUTO,
      numero: 'AE-2026-000001',
      referenceDevis: REFERENCE,
      produit: 'HABITATION',
      statut: 'EN_ATTENTE_DE_SIGNATURE',
      formule: 'confort',
      vehicule: null,
      logement: { type: 'appartement', surface: 45, adresse: '12 rue des Lilas', codePostal: '75001', ville: 'Paris' },
      garanties: [],
      documents: [],
    });

    expect(page.textContent).toContain('Logement assuré');
    expect(page.textContent).toContain('12 rue des Lilas, 75001 Paris');
    expect(page.textContent).toContain('Aucune option');
    expect(page.textContent).toContain('Vos documents seront disponibles ici dès leur création.');
    const signer = Array.from(page.querySelectorAll('a')).find((lien) => lien.textContent?.includes('Signer'));
    expect(signer?.getAttribute('href')).toBe(`/devis/contrat?reference=${REFERENCE}`);
  });

  describe('téléchargement des documents', () => {
    beforeEach(() => {
      ouvrir('AE-RECETTE-A03');
      repondreFiche('AE-RECETTE-A03', FICHE_AUTO);
    });

    const telecharger = (indice: number): void => {
      page.querySelectorAll<HTMLButtonElement>('.fiche__documents button')[indice].click();
    };

    it('télécharge les conditions particulières par la route de T-03', () => {
      vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:cp');
      vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
      const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
      let nom = '';
      clic.mockImplementation(function (this: HTMLAnchorElement) {
        nom = this.download;
      });

      telecharger(0);
      http.expectOne('/api/assurance-iard-api/documents-contrat/12').flush(new Blob(['%PDF']));

      expect(nom).toBe('conditions-particulieres-AE-RECETTE-A03-v1.pdf');
      vi.restoreAllMocks();
    });

    it('409 : « Document en préparation, réessayez dans quelques minutes »', () => {
      telecharger(1);
      http
        .expectOne('/api/assurance-iard-api/documents-contrat/13')
        .flush(new Blob(), { status: 409, statusText: 'x' });
      fixture.detectChanges();

      expect(page.querySelector('[role="status"]')?.textContent).toBe(
        'Document en préparation, réessayez dans quelques minutes.',
      );
    });

    it('autre erreur : message clair', () => {
      telecharger(0);
      http
        .expectOne('/api/assurance-iard-api/documents-contrat/12')
        .flush(new Blob(), { status: 500, statusText: 'x' });
      fixture.detectChanges();

      expect(page.querySelector('[role="status"]')?.textContent).toContain("n'a pas pu être téléchargé");
    });
  });

  it('dit si le contrat est introuvable', () => {
    ouvrir('AE-INCONNU');
    repondreFiche('AE-INCONNU', {}, 404);
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('introuvable');
  });

  it("affiche un message clair si le contrat n'a pas pu être chargé", () => {
    ouvrir('AE-RECETTE-A03');
    repondreFiche('AE-RECETTE-A03', {}, 500);
    expect(page.querySelector('[role="alert"]')?.textContent).toContain("n'a pas pu être chargé");
  });

  describe('non-régression : ancienne adresse par référence de devis', () => {
    it('redirige vers la fiche par numéro du même contrat, sans garder l’ancienne adresse dans l’historique', () => {
      ouvrir(REFERENCE);
      http
        .expectOne(`/api/assurance-iard-api/devis/contrat/${REFERENCE}`)
        .flush({ numero: 'AE-2026-000001', statut: 'ACTIF' });

      expect(router.navigate).toHaveBeenCalledWith(['/espace-client/contrats', 'AE-2026-000001'], {
        replaceUrl: true,
      });

      // La redirection change le paramètre : la fiche par numéro se charge.
      parametres.next(convertToParamMap({ numero: 'AE-2026-000001' }));
      repondreFiche('AE-2026-000001', { ...FICHE_AUTO, numero: 'AE-2026-000001' });
      expect(page.querySelector('h1')?.textContent).toContain('AE-2026-000001');
    });

    it("une référence inconnue ou d'un autre client ne redirige pas (le refus 403 est affiché par l'intercepteur)", () => {
      ouvrir(REFERENCE);
      http.expectOne(`/api/assurance-iard-api/devis/contrat/${REFERENCE}`).flush({}, { status: 404, statusText: 'x' });
      fixture.detectChanges();

      expect(router.navigate).not.toHaveBeenCalled();
      expect(page.querySelector('[role="alert"]')?.textContent).toContain('introuvable');
    });

    it('erreur technique pendant la redirection : message clair', () => {
      ouvrir(REFERENCE);
      http.expectOne(`/api/assurance-iard-api/devis/contrat/${REFERENCE}`).flush({}, { status: 500, statusText: 'x' });
      fixture.detectChanges();

      expect(page.querySelector('[role="alert"]')?.textContent).toContain("n'a pas pu être chargé");
    });
  });
});
