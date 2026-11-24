import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CONTRAT_AUTO, CONTRAT_HABITATION } from '../../core/model/contrat.exemples';
import { ConnexionService } from '../../core/service/connexion.service';
import { MesDocuments } from './mes-documents';

describe('MesDocuments (US-04, AC1)', () => {
  let fixture: ComponentFixture<MesDocuments>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MesDocuments);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  const repondre = (corps: object, status = 200): void => {
    http.expectOne('/api/assurance-iard-api/documents').flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };

  it('US-18 : n’a plus son propre en-tête (le cadre commun est apporté par la route)', () => {
    repondre([]);
    expect(page.querySelector('header')).toBeNull();
    expect(page.querySelector('main')).toBeNull();
    expect(Array.from(page.querySelectorAll('a')).map((a) => a.textContent?.trim())).not.toContain('Mon espace');
  });

  const doc = (id: number, referenceDevis: string, categorie: string, nomFichier: string) => ({
    id,
    referenceDevis,
    categorie,
    typeDocument: 'x',
    nomFichier,
    typeFichier: 'pdf',
    taille: 3,
  });
  const REF_A = '7c9e6679-7425-40de-944b-e07fc1f90ae7';
  const REF_B = 'a1b2c3d4-0000-4000-8000-000000000001';

  it('liste mes documents, une ligne compacte par document avec un lien « Ouvrir »', () => {
    expect(page.textContent).toContain('Chargement de vos documents');
    repondre([doc(5, REF_A, 'identite', 'Sans titre.png')]);

    const ligne = page.querySelector('.document')!;
    expect(ligne.querySelector('.document__type')?.textContent).toBe("Pièce d'identité");
    expect(ligne.querySelector('.document__fichier')?.textContent).toBe('Sans titre.png');
    expect(ligne.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    const ouvrir = ligne.querySelector('a')!;
    expect(ouvrir.textContent?.trim()).toBe('Ouvrir');
    expect(ouvrir.getAttribute('href')).toBe('/espace-client/documents/5');
    expect(ouvrir.getAttribute('aria-label')).toBe("Ouvrir : Pièce d'identité, Sans titre.png");
    // Simple lien, plus de gros bouton ni de carte par document.
    expect(ouvrir.classList).not.toContain('btn');
    expect(page.querySelectorAll('.devis-ligne')).toHaveLength(0);
    expect(page.querySelectorAll('.documents')).toHaveLength(1);
  });

  describe('regroupement par souscription, titré par le contrat obtenu', () => {
    const afficherAvecContrats = (contrats: object[] | null): void => {
      TestBed.inject(ConnexionService).droitsClient.set(['CONTRATS:CONSULTER', 'DOCUMENTS:CONSULTER']);
      fixture = TestBed.createComponent(MesDocuments);
      fixture.detectChanges();
      page = fixture.nativeElement as HTMLElement;
      http.expectOne('/api/assurance-iard-api/documents').flush([
        doc(1, REF_A, 'identite', 'cni.pdf'),
        doc(2, REF_B, 'domicile', 'facture-edf.pdf'),
        doc(3, REF_A, 'domicile', 'quittance.pdf'),
      ]);
      const requete = http.expectOne('/api/assurance-iard-api/contrats');
      if (contrats === null) {
        requete.flush({}, { status: 500, statusText: 'x' });
      } else {
        requete.flush(contrats);
      }
      fixture.detectChanges();
    };
    const titres = (): (string | undefined)[] =>
      Array.from(page.querySelectorAll('.documents__groupe h2')).map((t) => t.textContent?.trim());

    beforeEach(() => {
      // Le composant créé par le beforeEach général a demandé ses documents : on y répond pour repartir de zéro.
      repondre([]);
    });

    it('« Contrat <numéro> » si la souscription a donné un contrat, sinon « Souscription en cours »', () => {
      afficherAvecContrats([{ ...CONTRAT_HABITATION, referenceDevis: REF_A, numero: 'AE-RECETTE-1' }, CONTRAT_AUTO]);

      expect(titres()).toEqual(['Contrat AE-RECETTE-1', 'Souscription en cours']);
      const groupes = Array.from(page.querySelectorAll('.documents__groupe'));
      expect(Array.from(groupes[0].querySelectorAll('.document__fichier')).map((f) => f.textContent)).toEqual([
        'cni.pdf',
        'quittance.pdf',
      ]);
      expect(groupes[1].querySelector('.document__type')?.textContent).toBe('Justificatif de domicile');
    });

    it('aucune référence technique n’est affichée au client', () => {
      afficherAvecContrats([]);
      expect(page.textContent).not.toContain(REF_A.slice(0, 8));
      expect(page.textContent?.toUpperCase()).not.toContain('RÉF.');
    });

    it('si les contrats n’ont pas pu être lus, les documents restent affichés', () => {
      afficherAvecContrats(null);
      expect(titres()).toEqual(['Souscription en cours', 'Souscription en cours']);
      expect(page.querySelectorAll('.document')).toHaveLength(3);
    });
  });

  it('sans le droit de consulter les contrats, ne les demande pas (pas de refus d’accès)', () => {
    repondre([doc(1, REF_A, 'identite', 'cni.pdf')]);
    http.expectNone('/api/assurance-iard-api/contrats');
    expect(page.querySelector('.documents__groupe h2')?.textContent?.trim()).toBe('Souscription en cours');
  });

  it("dit quand il n'y a aucun document", () => {
    repondre([]);
    expect(page.textContent).toContain("Vous n'avez pas encore déposé de document.");
  });

  it('affiche un message clair si le chargement échoue', () => {
    repondre({}, 500);
    expect(page.querySelector('[role="alert"]')).not.toBeNull();
  });
});
