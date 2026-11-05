import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { formaterDate, premierPrelevement } from '../../core/model/premier-prelevement';
import { DevisService } from '../../core/service/devis.service';
import { DevisPaiement } from './devis-paiement';

const URL_API = '/api/devis/mandats';
const IBAN_TEST = 'FR7630006000011234567890189';
const IBAN_FAUX = 'FR7630006000011234567890188';
const MANDAT = { creancier: "Assur'Easy", ics: 'FR00ZZZ000000', rum: 'AE-2026-ABCDEFGH' };

const dansJours = (jours: number): Date => {
  const maintenant = new Date();
  return new Date(maintenant.getFullYear(), maintenant.getMonth(), maintenant.getDate() + jours);
};

describe('DevisPaiement', () => {
  let fixture: ComponentFixture<DevisPaiement>;
  let page: HTMLElement;
  let devis: DevisService;
  let router: Router;
  let http: HttpTestingController;

  const ouvrirEcran = (): void => {
    fixture = TestBed.createComponent(DevisPaiement);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
    document.body.appendChild(page);
  };

  const creerEcran = (): void => {
    ouvrirEcran();
    http.expectOne(URL_API).flush(MANDAT);
    fixture.detectChanges();
  };

  const champ = <T extends HTMLElement = HTMLInputElement>(id: string): T => page.querySelector<T>(`#${id}`)!;

  const saisir = (id: string, valeur: string): void => {
    const element = champ(id);
    element.value = valeur;
    element.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  const quitter = (id: string): void => {
    champ(id).dispatchEvent(new Event('blur'));
    fixture.detectChanges();
  };

  const bouton = (texte: string): HTMLButtonElement =>
    Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === texte)!;

  const choisirJour = (jour: number): void => {
    bouton(`Le ${jour}`).click();
    fixture.detectChanges();
  };

  const autoriser = (): void => {
    champ('autorisation').click();
    fixture.detectChanges();
  };

  const remplir = (): void => {
    saisir('date-debut', formaterDate(dansJours(30)));
    saisir('iban', IBAN_TEST);
    choisirJour(10);
    autoriser();
  };

  const generer = (): void => {
    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  };

  const boutonGenerer = (): HTMLButtonElement => page.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const erreurs = (): string[] =>
    Array.from(page.querySelectorAll('.erreur')).map((e) => e.textContent?.trim() ?? '');

  const lignesPaiement = (): Record<string, string> => {
    const bloc = Array.from(page.querySelectorAll('.bloc')).find((b) => b.textContent?.includes('Mon paiement'))!;
    return Object.fromEntries(
      Array.from(bloc.querySelectorAll('.bloc__ligne')).map((l) => [
        l.querySelector('dt')?.textContent?.trim(),
        l.querySelector('dd')?.textContent?.trim(),
      ]),
    );
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    devis = TestBed.inject(DevisService);
    devis.mettreAJour({ surface: '60', typeLogement: 'appartement', statut: 'locataire' });
    devis.validerRecommandation('ref-1', 'confort');
    devis.franchiseChoisie.set('300');
    devis.informations.update((s) => ({ ...s, civilite: 'madame', nom: 'Durand ', prenom: ' Marie' }));
    devis.referenceDossier.set('ref-1');
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    page.remove();
  });

  describe('AC1 : informations de prélèvement', () => {
    beforeEach(() => creerEcran());

    it('pré-remplit seulement le titulaire avec le nom du client', () => {
      expect(champ('titulaire').value).toBe('Marie Durand');
      expect(champ('date-debut').value).toBe('');
      expect(champ('iban').value).toBe('');
    });

    it('RG3 : propose le 5, le 10 ou le 15, sans présélection', () => {
      expect(Array.from(page.querySelectorAll('.choix__option')).map((b) => b.textContent?.trim())).toEqual([
        'Le 5',
        'Le 10',
        'Le 15',
      ]);
      expect(page.querySelector('[aria-pressed="true"]')).toBeNull();
    });

    it('met en évidence le jour choisi', () => {
      choisirJour(15);
      expect(bouton('Le 15').getAttribute('aria-pressed')).toBe('true');
      expect(page.querySelectorAll('[aria-pressed="true"]')).toHaveLength(1);
    });

    it('présente l’IBAN par groupes et la date avec ses barres pendant la frappe', () => {
      saisir('iban', 'fr7630006000');
      saisir('date-debut', '01122026');

      expect(champ('iban').value).toBe('FR76 3000 6000');
      expect(champ('date-debut').value).toBe('01/12/2026');
    });

    it('reprend les informations dans le récapitulatif, IBAN masqué', () => {
      const debut = formaterDate(dansJours(30));
      remplir();

      const lignes = lignesPaiement();
      expect(lignes['Date de début']).toBe(debut);
      expect(lignes['Titulaire du compte']).toBe('Marie Durand');
      expect(lignes['IBAN']?.endsWith('0189')).toBe(true);
      expect(lignes['IBAN']).not.toContain('FR76');
      expect(lignes['Jour de prélèvement']).toBe('Le 10 de chaque mois');
      expect(page.textContent).not.toContain(IBAN_TEST);
    });

    it('RG4 : affiche la date du premier prélèvement', () => {
      remplir();

      expect(lignesPaiement()['Premier prélèvement']).toBe(
        formaterDate(premierPrelevement(dansJours(30), 10, new Date())),
      );
      expect(page.textContent).toContain('au moins 14 jours avant le premier prélèvement');
    });

    it('n’annonce rien tant que les informations manquent ou sont fausses', () => {
      saisir('titulaire', '');
      saisir('iban', IBAN_FAUX);

      expect(lignesPaiement()['IBAN']).toBe('À renseigner');
      expect(lignesPaiement()['Titulaire du compte']).toBe('À renseigner');
      expect(lignesPaiement()['Premier prélèvement']).toBe('À renseigner');
      expect(lignesPaiement()['Jour de prélèvement']).toBe('À renseigner');
    });
  });

  describe('AC2 : mandat SEPA', () => {
    it('affiche le créancier, l’ICS, la RUM, le type de paiement et les droits de remboursement', () => {
      creerEcran();
      const encadre = page.querySelector('.encadre-mandat')?.textContent;

      expect(encadre).toContain('Mandat de prélèvement SEPA');
      expect(encadre).toContain("Assur'Easy");
      expect(encadre).toContain('FR00ZZZ000000');
      expect(encadre).toContain('AE-2026-ABCDEFGH');
      expect(encadre).toContain('Récurrent');
      expect(encadre).toContain('8 semaines');
      expect(encadre).toContain('13 mois');
    });

    it('prépare le mandat pour le devis en cours', () => {
      ouvrirEcran();
      const requete = http.expectOne(URL_API);
      expect(requete.request.body).toEqual({ referenceDevis: 'ref-1' });
      requete.flush(MANDAT);
    });

    it('prévient et bloque le bouton si le mandat ne peut pas être préparé', () => {
      ouvrirEcran();
      http.expectOne(URL_API).flush(null, { status: 500, statusText: 'Erreur' });
      fixture.detectChanges();
      autoriser();

      expect(page.querySelector('.alerte')?.textContent).toContain('pas pu être préparé');
      expect(boutonGenerer().disabled).toBe(true);
      generer();
      http.expectNone(`${URL_API}/signature`);
    });

    it('renvoie à la connexion si la session a expiré à l’arrivée', () => {
      ouvrirEcran();
      http.expectOne(URL_API).flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(router.navigate).toHaveBeenCalledWith(['/connexion'], { queryParams: { retour: '/devis/paiement' } });
    });
  });

  describe('AC3 et RG5 : autorisation', () => {
    beforeEach(() => creerEcran());

    it('n’est jamais pré-cochée, même au retour sur l’écran', () => {
      expect(champ('autorisation').checked).toBe(false);
      autoriser();
      page.remove();

      creerEcran();
      expect(champ('autorisation').checked).toBe(false);
    });

    it('bloque le bouton et invite à cocher', () => {
      expect(boutonGenerer().disabled).toBe(true);
      expect(page.querySelector('.invitation')?.textContent).toContain('Cochez l\'autorisation');
    });

    it('active le bouton une fois cochée, et le bloque de nouveau si elle est décochée', () => {
      autoriser();
      expect(boutonGenerer().disabled).toBe(false);
      expect(page.querySelector('.invitation')).toBeNull();

      autoriser();
      expect(boutonGenerer().disabled).toBe(true);
    });

    it('n’envoie rien sans autorisation', () => {
      saisir('date-debut', formaterDate(dansJours(30)));
      saisir('iban', IBAN_TEST);
      choisirJour(10);
      generer();

      http.expectNone(`${URL_API}/signature`);
      expect(erreurs()).toEqual([]);
    });
  });

  describe('AC4 : contrôles', () => {
    beforeEach(() => creerEcran());

    it('n’affiche aucune erreur avant que le client n’agisse', () => {
      expect(erreurs()).toEqual([]);
    });

    it('signale une clé IBAN fausse dès que le client quitte le champ', () => {
      saisir('iban', IBAN_FAUX);
      expect(erreurs()).toEqual([]);

      quitter('iban');
      expect(page.querySelector('#erreur-iban')?.textContent).toContain("n'est pas valide");
      expect(champ('iban').getAttribute('aria-invalid')).toBe('true');
    });

    it.each([
      ['passée', -1, 'au plus tôt demain'],
      ['égale à aujourd’hui', 0, 'au plus tôt demain'],
      ['à plus de 90 jours', 91, '90 prochains jours'],
    ])('refuse une date de début %s et y place le curseur', (_cas, jours, message) => {
      remplir();
      saisir('date-debut', formaterDate(dansJours(jours)));

      generer();

      expect(erreurs()[0]).toContain(message);
      expect(document.activeElement).toBe(champ('date-debut'));
      http.expectNone(`${URL_API}/signature`);
    });

    it('accepte le lendemain et le 90e jour', () => {
      saisir('date-debut', formaterDate(dansJours(1)));
      quitter('date-debut');
      expect(erreurs()).toEqual([]);

      saisir('date-debut', formaterDate(dansJours(90)));
      expect(erreurs()).toEqual([]);
    });

    it('place le curseur sur l’IBAN s’il est faux', () => {
      remplir();
      saisir('iban', IBAN_FAUX);

      generer();

      expect(erreurs()).toEqual(["Cet IBAN n'est pas valide. Vérifiez sa saisie."]);
      expect(document.activeElement).toBe(champ('iban'));
    });

    it('signale tous les champs vides, curseur sur le premier', () => {
      saisir('titulaire', ' ');
      autoriser();

      generer();

      expect(erreurs()).toEqual([
        'Indiquez la date de début de votre contrat.',
        'Indiquez le titulaire du compte.',
        'Indiquez votre IBAN.',
        'Choisissez votre jour de prélèvement.',
      ]);
      expect(document.activeElement).toBe(champ('date-debut'));
    });
  });

  describe('AC5 : récapitulatif', () => {
    it('présente les 5 blocs', () => {
      creerEcran();
      expect(Array.from(page.querySelectorAll('.bloc__titre')).map((t) => t.textContent)).toEqual([
        'Mon logement',
        'Mes besoins',
        'Ma formule',
        'Mon identité',
        'Mon paiement',
      ]);
    });

    it('relie chaque bloc à son étape', () => {
      creerEcran();
      const liens = Array.from(page.querySelectorAll<HTMLAnchorElement>('a.bloc__modifier'));

      expect(liens.map((a) => a.getAttribute('href'))).toEqual([
        '/devis/logement',
        '/devis/besoins',
        '/devis/formule',
        '/devis/informations',
      ]);
      expect(liens.every((a) => a.textContent?.trim() === 'Modifier')).toBe(true);
    });

    it('« Modifier » sur le paiement place le curseur sur le premier champ', () => {
      creerEcran();
      page.querySelector<HTMLButtonElement>('button.bloc__modifier')!.click();

      expect(document.activeElement).toBe(champ('date-debut'));
    });

    it('reprend la cotisation de la formule choisie', () => {
      creerEcran();
      expect(lignesPaiement()['Montant']).toContain('€/mois');
    });

    it('reprend les colocataires déclarés', () => {
      devis.mettreAJour({ statut: 'colocation', nombreColocataires: '2' });
      devis.informations.update((s) => ({ ...s, colocataires: ['Paul Martin', 'Ancien nom'] }));
      creerEcran();

      expect(page.textContent).toContain('Paul Martin');
      expect(page.textContent).not.toContain('Ancien nom');
    });

    it('conserve la saisie au retour, sauf l’autorisation', () => {
      creerEcran();
      remplir();
      page.remove();

      creerEcran();

      expect(champ('iban').value).toBe('FR76 3000 6000 0112 3456 7890 189');
      expect(bouton('Le 10').getAttribute('aria-pressed')).toBe('true');
      expect(champ('autorisation').checked).toBe(false);
    });

    it('garde un titulaire modifié par le client', () => {
      devis.paiement.update((s) => ({ ...s, titulaire: 'Paul Durand' }));
      creerEcran();

      expect(champ('titulaire').value).toBe('Paul Durand');
    });
  });

  it('AC6 : affiche le droit de renonciation', () => {
    creerEcran();
    expect(page.textContent).toContain('Droit de renonciation');
    expect(page.textContent).toContain('14 jours après la signature');
    expect(page.textContent).toContain('sans frais');
  });

  describe('signature', () => {
    beforeEach(() => creerEcran());

    const requete = () => http.expectOne(`${URL_API}/signature`);

    it('envoie le mandat et ouvre l’étape suivante', () => {
      remplir();
      generer();

      expect(boutonGenerer().disabled).toBe(true);
      const envoi = requete();
      expect(envoi.request.body).toEqual({
        referenceDevis: 'ref-1',
        titulaire: 'Marie Durand',
        iban: IBAN_TEST,
        dateDebut: formaterDate(dansJours(30)).split('/').reverse().join('-'),
        jourPrelevement: 10,
        autorisation: true,
      });
      envoi.flush({ rum: 'AE-2026-ABCDEFGH', datePremierPrelevement: '2026-12-10' });

      expect(router.navigate).toHaveBeenCalledWith(['/devis/contrat']);
    });

    it('n’envoie pas deux fois pendant l’enregistrement', () => {
      remplir();
      generer();
      generer();

      requete().flush({});
    });

    it('affiche sous le champ un refus du serveur, puis l’efface à la saisie suivante', () => {
      remplir();
      generer();

      requete().flush({ champ: 'iban', detail: 'Compte refusé.' }, { status: 422, statusText: 'Unprocessable' });
      fixture.detectChanges();

      expect(erreurs()).toEqual(['Compte refusé.']);
      expect(document.activeElement).toBe(champ('iban'));
      expect(router.navigate).not.toHaveBeenCalled();

      saisir('iban', 'DE89370400440532013000');
      expect(erreurs()).toEqual([]);
    });

    it('affiche un message général pour un refus sans champ connu', () => {
      remplir();
      generer();

      requete().flush({ champ: 'autre', detail: 'Refusé.' }, { status: 422, statusText: 'Unprocessable' });
      fixture.detectChanges();

      expect(page.querySelector('.alerte')?.textContent).toContain('pas pu être enregistré');
    });

    it('reste sur l’écran en cas d’échec, et permet de réessayer', () => {
      remplir();
      generer();

      requete().flush(null, { status: 500, statusText: 'Erreur' });
      fixture.detectChanges();
      expect(page.querySelector('.alerte')).not.toBeNull();
      expect(boutonGenerer().disabled).toBe(false);

      generer();
      expect(page.querySelector('.alerte')).toBeNull();
      requete().flush({});
    });

    it('renvoie à la connexion si la session a expiré, en retenant l’étape', () => {
      remplir();
      generer();

      requete().flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(router.navigate).toHaveBeenCalledWith(['/connexion'], { queryParams: { retour: '/devis/paiement' } });
    });
  });

  it('affiche l’étape en cours et le retour à l’étape 4', () => {
    creerEcran();

    expect(page.querySelector('h1')?.textContent).toContain('Paiement');
    expect(page.querySelector('[aria-current="step"]')?.textContent).toContain('Paiement');
    const lien = Array.from(page.querySelectorAll('a')).find((a) => a.textContent?.includes('Retour'));
    expect(lien?.getAttribute('href')).toBe('/devis/informations');
  });
});
