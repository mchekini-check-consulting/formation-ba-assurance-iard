import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContratFactures, FactureClient, MESSAGE_AUCUNE_COTISATION, MesFactures as Donnees } from '../../core/model/facture';
import { ConnexionService } from '../../core/service/connexion.service';
import { MESSAGE_EN_PREPARATION } from '../../core/service/documents-contrat.service';
import { MESSAGE_ERREUR_FACTURE } from '../../core/service/factures.service';
import { MesFactures, ancreContrat, messageGeneration } from './mes-factures';

const URL_LISTE = '/api/assurance-iard-api/espace-client/factures';
const URL_GENERER = '/api/assurance-iard-api/contrats/AE-RECETTE-1/factures';

const FACTURE: FactureClient = {
  numero: 'FAC-2026-000042',
  periodeDebut: '2026-11-10',
  periodeFin: '2026-12-09',
  montantHt: 16.97,
  taxe: 1.53,
  montantTtc: 18.5,
  genereeLe: '2026-11-25T10:00:00Z',
  documentId: 7,
};

function contrat(modif: Partial<ContratFactures> = {}): ContratFactures {
  return {
    numero: 'AE-RECETTE-1',
    produit: 'HABITATION',
    statut: 'ACTIF',
    bienAssure: '12 rue des Lilas, 75001 Paris',
    echeances: [
      { date: '2026-12-10', montantTtc: 18.5, statut: 'A_VENIR', periodeDebut: '2026-12-10', periodeFin: '2027-01-09', facturable: true },
      { date: '2026-11-10', montantTtc: 18.5, statut: 'PAYEE', periodeDebut: '2026-11-10', periodeFin: '2026-12-09', facturable: true },
      { date: '2026-10-10', montantTtc: 18.5, statut: 'IMPAYEE', periodeDebut: '2026-10-10', periodeFin: '2026-11-09', facturable: false },
    ],
    annees: [{ debut: '2026-03-01', fin: '2027-02-28', montantTtc: 222, facturable: true }],
    factures: [],
    ...modif,
  };
}

describe('MesFactures (US-22)', () => {
  let fixture: ComponentFixture<MesFactures>;
  let page: HTMLElement;
  let http: HttpTestingController;

  const ouvrir = (droits = ['CONTRATS:CONSULTER', 'DOCUMENTS:CONSULTER', 'DOCUMENTS:CREER']): void => {
    TestBed.inject(ConnexionService).droitsClient.set(droits);
    fixture = TestBed.createComponent(MesFactures);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };

  const repondre = (corps: Donnees | null, status = 200): void => {
    http.expectOne(URL_LISTE).flush(corps, { status, statusText: 'x' });
    fixture.detectChanges();
  };

  const boutons = (libelle: string): HTMLButtonElement[] =>
    Array.from(page.querySelectorAll<HTMLButtonElement>('button')).filter((b) => b.textContent?.trim() === libelle);

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  it('affiche le chargement, puis un message compréhensible si les factures ne viennent pas', () => {
    ouvrir();
    expect(page.textContent).toContain('Chargement de vos factures');
    repondre(null, 500);
    expect(page.querySelector('[role="alert"]')?.textContent).toContain("Vos factures n'ont pas pu être chargées");
  });

  it('AC4 : aucune cotisation émise', () => {
    ouvrir();
    repondre({ contrats: [], aucuneCotisation: true });
    expect(page.textContent).toContain(MESSAGE_AUCUNE_COTISATION);
    expect(page.querySelectorAll('button')).toHaveLength(0);
  });

  it('AC1 : échéancier de chaque contrat, dates, montants TTC et statuts, du plus récent au plus ancien', () => {
    ouvrir();
    repondre({ contrats: [contrat({ factures: [FACTURE] })], aucuneCotisation: false });

    const section = page.querySelector(`#${ancreContrat('AE-RECETTE-1')}`)!;
    expect(section.querySelector('h2')?.textContent).toContain('Contrat AE-RECETTE-1 · Habitation');
    const echeancier = section.querySelector('[aria-label="Échéancier du contrat AE-RECETTE-1"]')!;
    const lignes = Array.from(echeancier.querySelectorAll('li'));
    expect(lignes.map((l) => l.querySelector('.ligne__date')?.textContent)).toEqual([
      '10/12/2026',
      '10/11/2026',
      '10/10/2026',
    ]);
    expect(lignes[0].textContent).toContain('18,50');
    expect(lignes.map((l) => l.querySelector('.statut-echeance')?.textContent?.trim())).toEqual([
      'À venir',
      'Payée',
      'Impayée',
    ]);
    // Une période non facturable (après la fin d'un contrat résilié) n'a pas de bouton.
    expect(lignes[2].querySelector('button')).toBeNull();
    expect(section.textContent).toContain('Du 01/03/2026 au 28/02/2027');
    expect(section.textContent).toContain('FAC-2026-000042');
    expect(page.querySelector('[aria-label="Télécharger la facture FAC-2026-000042"]')).not.toBeNull();
  });

  it('un contrat sans échéance ni facture le dit', () => {
    ouvrir();
    repondre({ contrats: [contrat({ echeances: [], annees: [] })], aucuneCotisation: false });
    expect(page.textContent).toContain('Aucune échéance pour ce contrat pour le moment.');
    expect(page.textContent).toContain('Aucune facture générée pour ce contrat.');
  });

  it('sans le droit de créer des documents, pas de bouton « Générer une facture »', () => {
    ouvrir(['CONTRATS:CONSULTER', 'DOCUMENTS:CONSULTER']);
    repondre({ contrats: [contrat()], aucuneCotisation: false });
    expect(boutons('Générer une facture')).toHaveLength(0);
  });

  it('AC2 : génère la facture d’une échéance, la télécharge et l’ajoute à la liste', () => {
    ouvrir();
    repondre({ contrats: [contrat()], aucuneCotisation: false });
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:facture');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    boutons('Générer une facture')[1].click();
    fixture.detectChanges();
    expect(page.textContent).toContain('Génération…');
    expect(boutons('Générer une facture').every((b) => b.disabled)).toBe(true);

    const requete = http.expectOne(URL_GENERER);
    expect(requete.request.body).toEqual({ periode: 'ECHEANCE', date: '2026-11-10' });
    requete.flush({ facture: FACTURE, disponible: true, existante: false });
    http.expectOne('/api/assurance-iard-api/documents-contrat/7').flush(new Blob(['%PDF']));
    fixture.detectChanges();

    expect(clic).toHaveBeenCalled();
    expect(page.querySelector('[role="status"]')?.textContent).toContain(
      messageGeneration('FAC-2026-000042', false, true),
    );
    const factures = page.querySelector('[aria-label="Factures du contrat AE-RECETTE-1"]')!;
    expect(factures.textContent).toContain('FAC-2026-000042');
  });

  it('AC2 : génère la facture d’une année de cotisation ; déjà générée, la même est renvoyée sans doublon', () => {
    ouvrir();
    repondre({ contrats: [contrat({ factures: [{ ...FACTURE, documentId: null }] })], aucuneCotisation: false });

    const annuel = page.querySelector<HTMLButtonElement>(
      '[aria-label="Générer la facture de l\'année du 01/03/2026 au 28/02/2027"]',
    )!;
    annuel.click();
    const requete = http.expectOne(URL_GENERER);
    expect(requete.request.body).toEqual({ periode: 'ANNEE', date: '2026-03-01' });
    requete.flush({ facture: { ...FACTURE, documentId: null }, disponible: false, existante: true });
    fixture.detectChanges();

    expect(page.textContent).toContain(messageGeneration('FAC-2026-000042', true, false));
    expect(page.querySelectorAll('[aria-label="Factures du contrat AE-RECETTE-1"] li')).toHaveLength(1);
    expect(page.textContent).toContain('En préparation');
  });

  it('affiche le refus du serveur ou une erreur technique', () => {
    ouvrir();
    repondre({ contrats: [contrat()], aucuneCotisation: false });

    boutons('Générer une facture')[0].click();
    http.expectOne(URL_GENERER).flush({ detail: 'Pas pour cette période.' }, { status: 422, statusText: 'x' });
    fixture.detectChanges();
    expect(page.querySelector('.mes-devis__erreur')?.textContent).toContain('Pas pour cette période.');

    boutons('Générer une facture')[0].click();
    http.expectOne(URL_GENERER).flush(null, { status: 500, statusText: 'x' });
    fixture.detectChanges();
    expect(page.querySelector('.mes-devis__erreur')?.textContent).toContain(MESSAGE_ERREUR_FACTURE);
  });

  it('téléchargement d’une facture encore en préparation', () => {
    ouvrir();
    repondre({ contrats: [contrat({ factures: [FACTURE] })], aucuneCotisation: false });

    page.querySelector<HTMLButtonElement>('[aria-label="Télécharger la facture FAC-2026-000042"]')!.click();
    http.expectOne('/api/assurance-iard-api/documents-contrat/7').flush(new Blob(), { status: 409, statusText: 'x' });
    fixture.detectChanges();
    expect(page.textContent).toContain(MESSAGE_EN_PREPARATION);
  });

  it('messages de génération', () => {
    expect(messageGeneration('FAC-1', false, true)).toBe('Votre facture FAC-1 est générée. Son téléchargement commence.');
    expect(messageGeneration('FAC-1', true, false)).toBe(
      `Cette facture a déjà été générée : FAC-1. ${MESSAGE_EN_PREPARATION}`,
    );
  });
});
