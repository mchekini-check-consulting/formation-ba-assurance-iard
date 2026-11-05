import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { logementVide } from '../../core/model/devis';
import { DevisEnregistre, RepriseDevis } from '../../core/model/devis-enregistre';
import { reponsesVides } from '../../core/model/recommandation';
import { DevisService } from '../../core/service/devis.service';
import { MesDevis } from './mes-devis';

describe('MesDevis', () => {
  let fixture: ComponentFixture<MesDevis>;
  let page: HTMLElement;
  let http: HttpTestingController;
  let router: Router;

  const valide: DevisEnregistre = {
    reference: 'ref-1',
    formule: 'confort',
    typeLogement: 'appartement',
    surface: 60,
    adresse: '12 rue des Lilas',
    codePostal: '75001',
    ville: 'Paris',
    prixMensuel: 14.3,
    prixAnnuel: 171.6,
    dateFinValidite: '2026-12-05',
    expire: false,
  };
  const expire: DevisEnregistre = { ...valide, reference: 'ref-2', expire: true, dateFinValidite: '2026-01-05' };
  const reprise: RepriseDevis = {
    reference: 'ref-2',
    logement: logementVide(),
    besoins: reponsesVides(),
    formule: 'confort',
    options: [],
    franchise: '300',
    prixMensuel: 14.3,
    ancienPrixMensuel: 12.5,
    dateFinValidite: '2026-12-05',
  };

  const afficher = (liste: DevisEnregistre[]): void => {
    http.expectOne('/api/devis/enregistres').flush(liste);
    fixture.detectChanges();
  };
  const cliquer = (texte: string, rang = 0): void => {
    const boutons = Array.from(page.querySelectorAll('button')).filter((b) => b.textContent?.trim() === texte);
    boutons[rang].click();
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(MesDevis);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  it('AC1 : affiche le devis et sa date de fin de validité', () => {
    afficher([valide]);
    expect(page.textContent).toContain('Formule Confort');
    expect(page.textContent).toContain('Fin de validité : 05/12/2026');
    expect(page.querySelector('.badge')?.textContent).toBe('Valide');
  });

  it('AC6 : affiche « Expiré » au-delà de la validité', () => {
    afficher([expire]);
    expect(page.querySelector('.badge--expire')?.textContent).toBe('Expiré');
  });

  it('indique quand il n’y a aucun devis', () => {
    afficher([]);
    expect(page.textContent).toContain('pas encore de devis');
  });

  it('informe de la durée de conservation', () => {
    afficher([valide]);
    expect(page.textContent).toContain('conservé 3 ans');
  });

  it('signale un échec de chargement', () => {
    http.expectOne('/api/devis/enregistres').flush(null, { status: 500, statusText: 'Erreur' });
    fixture.detectChanges();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('pas pu être chargés');
  });

  it('AC5 et AC6 : reprend le devis et prépare le message de recalcul', () => {
    afficher([expire]);
    cliquer('Reprendre');
    http.expectOne('/api/devis/enregistres/ref-2/reprise').flush(reprise);
    const devis = TestBed.inject(DevisService);
    expect(devis.reference()).toBe('ref-2');
    expect(devis.messageReprise()).toContain('de 12,50 € à 14,30 €');
    expect(router.navigate).toHaveBeenCalledWith(['/devis/formule']);
  });

  it('signale un devis impossible à reprendre', () => {
    afficher([valide]);
    cliquer('Reprendre');
    http.expectOne('/api/devis/enregistres/ref-1/reprise').flush(null, { status: 404, statusText: 'Inconnu' });
    fixture.detectChanges();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('pas pu être repris');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('RGPD : demande confirmation, puis supprime le devis', () => {
    afficher([valide, expire]);
    cliquer('Supprimer');
    http.expectNone('/api/devis/enregistres/ref-1');
    cliquer('Confirmer la suppression');
    http.expectOne('/api/devis/enregistres/ref-1').flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();
    expect(page.querySelectorAll('.devis-ligne')).toHaveLength(1);
  });

  it('RGPD : la suppression peut être annulée', () => {
    afficher([valide]);
    cliquer('Supprimer');
    cliquer('Annuler');
    expect(page.querySelectorAll('.devis-ligne')).toHaveLength(1);
    expect(page.textContent).not.toContain('définitivement');
  });

  it('garde le devis si la suppression échoue', () => {
    afficher([valide]);
    cliquer('Supprimer');
    cliquer('Confirmer la suppression');
    http.expectOne('/api/devis/enregistres/ref-1').flush(null, { status: 500, statusText: 'Erreur' });
    fixture.detectChanges();
    expect(page.querySelectorAll('.devis-ligne')).toHaveLength(1);
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('pas pu être supprimé');
  });
});
