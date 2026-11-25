import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  AnneeCotisation,
  ContratFactures,
  FactureClient,
  MESSAGE_ANNEE_NON_REGLEE,
  MESSAGE_AUCUNE_COTISATION,
  MesFactures as Donnees,
} from '../../core/model/facture';
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

/** Comme FAC-RECETTE-0001 : un mois civil qui couvre l'échéance du 10/10. */
const FACTURE_OCTOBRE: FactureClient = {
  ...FACTURE,
  numero: 'FAC-RECETTE-0001',
  periodeDebut: '2026-10-01',
  periodeFin: '2026-10-31',
  documentId: 50,
};

/** Année en cours, pas entièrement réglée. */
const ANNEE_EN_COURS: AnneeCotisation = {
  debut: '2026-03-01',
  fin: '2027-02-28',
  montantTtc: 222,
  facturable: false,
  reglee: false,
  facture: null,
};

/** Année précédente, entièrement réglée. */
const ANNEE_REGLEE: AnneeCotisation = {
  debut: '2025-03-01',
  fin: '2026-02-28',
  montantTtc: 222,
  facturable: true,
  reglee: true,
  facture: null,
};

function contrat(modif: Partial<ContratFactures> = {}): ContratFactures {
  return {
    numero: 'AE-RECETTE-1',
    produit: 'HABITATION',
    statut: 'ACTIF',
    bienAssure: '12 rue des Lilas, 75001 Paris',
    echeances: [
      { date: '2026-12-10', montantTtc: 18.5, statut: 'A_VENIR', periodeDebut: '2026-12-10', periodeFin: '2027-01-09', facturable: false, facture: null },
      { date: '2026-11-10', montantTtc: 18.5, statut: 'PAYEE', periodeDebut: '2026-11-10', periodeFin: '2026-12-09', facturable: true, facture: null },
      { date: '2026-10-10', montantTtc: 18.5, statut: 'IMPAYEE', periodeDebut: '2026-10-10', periodeFin: '2026-11-09', facturable: false, facture: null },
    ],
    annees: [ANNEE_EN_COURS],
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

  const lignesEcheancier = (): HTMLLIElement[] =>
    Array.from(page.querySelectorAll<HTMLLIElement>('[aria-label="Échéancier du contrat AE-RECETTE-1"] li'));

  const telechargerPdf = (): ReturnType<typeof vi.spyOn> => {
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:facture');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    return vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
  };

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
    const lignes = lignesEcheancier();
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
    // Recette point 1 : l'année affiche le total de ses 12 mensualités.
    expect(section.textContent).toContain('Du 01/03/2026 au 28/02/2027');
    expect(section.textContent).toContain('222,00');
    expect(section.textContent).toContain('FAC-2026-000042');
    expect(page.querySelector('[aria-label="Télécharger la facture FAC-2026-000042"]')).not.toBeNull();
  });

  it('recette point 2 : « Générer une facture » seulement pour une échéance payée', () => {
    ouvrir();
    repondre({ contrats: [contrat()], aucuneCotisation: false });

    const [aVenir, payee, impayee] = lignesEcheancier();
    expect(aVenir.querySelector('button')).toBeNull();
    expect(payee.querySelector('button')?.textContent?.trim()).toBe('Générer une facture');
    expect(impayee.querySelector('button')).toBeNull();
  });

  it('recette point 2 : une année en cours n’a pas de facture annuelle, avec un message', () => {
    ouvrir();
    repondre({ contrats: [contrat()], aucuneCotisation: false });

    const annees = page.querySelector('[aria-label="Années de cotisation du contrat AE-RECETTE-1"]')!;
    expect(annees.querySelector('button')).toBeNull();
    expect(annees.textContent).toContain(MESSAGE_ANNEE_NON_REGLEE);
  });

  it('recette point 3 : une échéance déjà couverte par une facture propose son téléchargement', () => {
    ouvrir();
    const echeances = contrat().echeances.map((e) =>
      e.date === '2026-10-10' ? { ...e, statut: 'PAYEE' as const, facturable: true, facture: 'FAC-RECETTE-0001' } : e,
    );
    repondre({ contrats: [contrat({ echeances, factures: [FACTURE_OCTOBRE] })], aucuneCotisation: false });
    const clic = telechargerPdf();

    const octobre = lignesEcheancier()[2];
    expect(octobre.textContent).not.toContain('Générer une facture');
    const telecharger = octobre.querySelector<HTMLButtonElement>(
      '[aria-label="Télécharger la facture FAC-RECETTE-0001 de l\'échéance du 10/10/2026"]',
    )!;
    expect(telecharger.textContent?.trim()).toBe('Télécharger la facture');
    telecharger.click();
    http.expectOne('/api/assurance-iard-api/documents-contrat/50').flush(new Blob(['%PDF']));
    expect(clic).toHaveBeenCalled();
  });

  it('une facture existante encore en préparation est signalée à la place du bouton', () => {
    ouvrir();
    const echeances = contrat().echeances.map((e) =>
      e.date === '2026-11-10' ? { ...e, facture: 'FAC-2026-000042' } : e,
    );
    repondre({
      contrats: [contrat({ echeances, factures: [{ ...FACTURE, documentId: null }] })],
      aucuneCotisation: false,
    });

    expect(lignesEcheancier()[1].textContent).toContain('Facture FAC-2026-000042 en préparation');
    expect(boutons('Générer une facture')).toHaveLength(0);
  });

  it('un contrat sans échéance ni facture le dit', () => {
    ouvrir();
    repondre({ contrats: [contrat({ echeances: [], annees: [] })], aucuneCotisation: false });
    expect(page.textContent).toContain('Aucune échéance pour ce contrat pour le moment.');
    expect(page.textContent).toContain('Aucune facture générée pour ce contrat.');
  });

  it('sans le droit de créer des documents, pas de bouton « Générer une facture »', () => {
    ouvrir(['CONTRATS:CONSULTER', 'DOCUMENTS:CONSULTER']);
    repondre({ contrats: [contrat({ annees: [ANNEE_REGLEE] })], aucuneCotisation: false });
    expect(boutons('Générer une facture')).toHaveLength(0);
  });

  it('AC2 : génère la facture d’une échéance payée, la télécharge, l’ajoute à la liste et à l’échéance', () => {
    ouvrir();
    repondre({ contrats: [contrat()], aucuneCotisation: false });
    const clic = telechargerPdf();

    boutons('Générer une facture')[0].click();
    fixture.detectChanges();
    expect(page.textContent).toContain('Génération…');

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
    // L'échéance propose maintenant le téléchargement, plus la génération.
    expect(lignesEcheancier()[1].textContent).toContain('Télécharger la facture');
    expect(boutons('Générer une facture')).toHaveLength(0);
  });

  it('AC2 : génère la facture d’une année entièrement réglée ; déjà générée, la même est renvoyée', () => {
    ouvrir();
    const annuelle: FactureClient = { ...FACTURE, periodeDebut: '2025-03-01', periodeFin: '2026-02-28', documentId: null };
    repondre({ contrats: [contrat({ annees: [ANNEE_EN_COURS, ANNEE_REGLEE] })], aucuneCotisation: false });

    page
      .querySelector<HTMLButtonElement>('[aria-label="Générer la facture de l\'année du 01/03/2025 au 28/02/2026"]')!
      .click();
    const requete = http.expectOne(URL_GENERER);
    expect(requete.request.body).toEqual({ periode: 'ANNEE', date: '2025-03-01' });
    requete.flush({ facture: annuelle, disponible: false, existante: true });
    fixture.detectChanges();

    expect(page.textContent).toContain(messageGeneration('FAC-2026-000042', true, false));
    expect(page.querySelectorAll('[aria-label="Factures du contrat AE-RECETTE-1"] li')).toHaveLength(1);
    const annees = page.querySelector('[aria-label="Années de cotisation du contrat AE-RECETTE-1"]')!;
    expect(annees.textContent).toContain('Facture FAC-2026-000042 en préparation');
  });

  it('une année déjà facturée propose le téléchargement de sa facture', () => {
    ouvrir();
    const annuelle: FactureClient = { ...FACTURE, numero: 'FAC-2026-000005', documentId: 9 };
    repondre({
      contrats: [contrat({ annees: [{ ...ANNEE_REGLEE, facture: 'FAC-2026-000005' }], factures: [annuelle] })],
      aucuneCotisation: false,
    });

    expect(
      page.querySelector('[aria-label="Télécharger la facture FAC-2026-000005 de l\'année du 01/03/2025"]'),
    ).not.toBeNull();
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
