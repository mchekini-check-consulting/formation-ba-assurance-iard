import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AnnonceService } from '../../core/service/annonce.service';
import { ConnexionService } from '../../core/service/connexion.service';
import { EspaceClientService } from '../../core/service/espace-client.service';
import { CadreEspace } from './cadre-espace';

@Component({ imports: [CadreEspace], template: '<iard-cadre-espace><p class="projete">Contenu</p></iard-cadre-espace>' })
class PageDEssai {}

const URL_TABLEAU = '/api/assurance-iard-api/espace-client/tableau-de-bord';

describe('CadreEspace : en-tête et barre latérale (US-18, maquette ; US-04)', () => {
  let fixture: ComponentFixture<PageDEssai>;
  let page: HTMLElement;
  let http: HttpTestingController;
  let connexion: ConnexionService;
  let router: Router;

  const afficherAvec = (profil: string, droits: string[]): void => {
    connexion.profilClient.set(profil);
    connexion.droitsClient.set(droits);
    fixture = TestBed.createComponent(PageDEssai);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };
  const liens = (selecteur: string): string[] =>
    Array.from(page.querySelectorAll(`${selecteur} a`)).map((lien) => lien.textContent?.trim() ?? '');

  beforeEach(() => {
    sessionStorage.setItem('assureasy.session', 'session');
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'espace-client', children: [] }, { path: 'espace-client/contrats', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    connexion = TestBed.inject(ConnexionService);
    router = TestBed.inject(Router);
    connexion.emailClient.set('ines@exemple.fr');
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('affiche le contenu de la page à côté de la navigation', () => {
    afficherAvec('ASSURE', []);
    expect(page.querySelector('main .projete')?.textContent).toBe('Contenu');
  });

  describe('en-tête', () => {
    it('initiales dans un rond, prénom et nom avec une majuscule initiale, même saisis en majuscules', () => {
      afficherAvec('ASSURE', ['CONTRATS:CONSULTER']);
      http.expectOne(URL_TABLEAU).flush({ prenom: 'INES', nom: 'BRAHIMI' });
      fixture.detectChanges();

      expect(page.querySelector('.compte__initiales')?.textContent?.trim()).toBe('IB');
      expect(page.querySelector('.compte__nom')?.textContent?.trim()).toBe('Ines Brahimi');
    });

    it("ne relit pas l'identité déjà connue pour ce compte", () => {
      TestBed.inject(EspaceClientService).identite.set({ email: 'ines@exemple.fr', prenom: 'Inès', nom: 'Brahimi' });
      afficherAvec('ASSURE', ['CONTRATS:CONSULTER']);

      http.expectNone(URL_TABLEAU);
      expect(page.querySelector('.compte__nom')?.textContent?.trim()).toBe('Inès Brahimi');
    });

    it("n'affiche jamais l'identité d'un autre compte : l'e-mail en attendant", () => {
      TestBed.inject(EspaceClientService).identite.set({ email: 'autre@exemple.fr', prenom: 'Bruno', nom: 'Petit' });
      afficherAvec('GESTIONNAIRE', []);

      expect(page.querySelector('.compte__nom')?.textContent?.trim()).toBe('ines@exemple.fr');
      expect(page.querySelector('.compte__initiales')).toBeNull();
    });

    it('« Se déconnecter » ferme la session, oublie l’identité et revient à l’accueil (US-07, AC3)', () => {
      TestBed.inject(EspaceClientService).identite.set({ email: 'ines@exemple.fr', prenom: 'Inès', nom: 'Brahimi' });
      afficherAvec('ASSURE', []);
      vi.spyOn(router, 'navigate').mockResolvedValue(true);

      page.querySelector<HTMLButtonElement>('.compte__deconnexion')!.click();
      const requete = http.expectOne('/api/assurance-iard-api/session');
      expect(requete.request.method).toBe('DELETE');
      requete.flush(null, { status: 204, statusText: 'No Content' });

      expect(TestBed.inject(EspaceClientService).identite()).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/']);
      expect(TestBed.inject(AnnonceService).prendre()).toBe('Vous êtes bien déconnecté.');
    });
  });

  describe('navigation client (ordre de la maquette, droits du profil)', () => {
    it('un assuré voit Tableau de bord, Mes contrats, Mes devis, Mes documents ; factures, sinistres et compte pas encore livrés', () => {
      afficherAvec('ASSURE', ['CONTRATS:CONSULTER', 'DEVIS:CONSULTER', 'DOCUMENTS:CONSULTER', 'SINISTRES:CONSULTER']);
      http.expectOne(URL_TABLEAU).flush({ prenom: 'Alice', nom: 'Martin' });

      expect(liens('.barre__navigation')).toEqual(['Tableau de bord', 'Mes contrats', 'Mes devis', 'Mes documents']);
    });

    it('une rubrique dont le droit est retiré disparaît (US-04, AC5)', () => {
      afficherAvec('ASSURE', ['DEVIS:CONSULTER']);

      expect(liens('.barre__navigation')).toEqual(['Tableau de bord', 'Mes devis']);
    });

    it('le lien actif est mis en évidence et annoncé (aria-current)', async () => {
      afficherAvec('ASSURE', ['DEVIS:CONSULTER']);
      await router.navigateByUrl('/espace-client');
      fixture.detectChanges();

      const actif = page.querySelector('.barre__lien--actif');
      expect(actif?.textContent?.trim()).toBe('Tableau de bord');
      expect(actif?.getAttribute('aria-current')).toBe('page');
      expect(page.querySelectorAll('.barre__lien--actif')).toHaveLength(1);
    });
  });

  describe('espace interne (US-04)', () => {
    it("n'apparaît jamais pour un assuré, même avec le droit « Comptes »", () => {
      afficherAvec('ASSURE', ['COMPTES_UTILISATEURS:CONSULTER', 'PROFILS:CONSULTER']);

      expect(page.querySelector('.interne')).toBeNull();
      expect(liens('.barre')).not.toContain('Dossiers clients');
    });

    it("n'apparaît jamais pour un prospect", () => {
      afficherAvec('PROSPECT', ['DEVIS:CONSULTER']);
      expect(page.querySelector('.interne')).toBeNull();
    });

    it("l'administrateur voit un bloc séparé « Espace interne » en bas de la barre latérale", () => {
      afficherAvec('ADMINISTRATEUR', ['COMPTES_UTILISATEURS:CONSULTER', 'PROFILS:CONSULTER']);

      expect(page.querySelector('.interne__titre')?.textContent).toBe('Espace interne');
      expect(liens('.interne')).toEqual(['Dossiers clients', 'Gérer les profils', 'Journal des accès refusés']);
      expect(liens('.barre__navigation')).not.toContain('Dossiers clients');
      expect(page.querySelector('.barre')?.lastElementChild?.classList).toContain('interne');
    });

    it('le gestionnaire voit les dossiers clients mais pas la gestion des profils', () => {
      afficherAvec('GESTIONNAIRE', ['COMPTES_UTILISATEURS:CONSULTER']);

      expect(liens('.interne')).toEqual(['Dossiers clients']);
    });
  });
});
