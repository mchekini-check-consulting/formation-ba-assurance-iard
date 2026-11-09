import { RECO_CONFORT } from '../../core/model/tarif.exemples';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { DevisService } from '../../core/service/devis.service';
import { DevisInformations } from './devis-informations';

const URL_API = '/api/assurance-iard-api/devis/informations';
const PDF = new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d, 0x31]);
const PNG = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

const vraiPdf = (nom = 'carte.pdf'): File => new File([PDF], nom);
const vraiPng = (nom = 'quittance.png'): File => new File([PNG], nom);

describe('DevisInformations', () => {
  let fixture: ComponentFixture<DevisInformations>;
  let page: HTMLElement;
  let devis: DevisService;
  let router: Router;
  let http: HttpTestingController;

  const rafraichir = async (): Promise<void> => {
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const creerEcran = (identite: object = {}): void => {
    fixture = TestBed.createComponent(DevisInformations);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
    document.body.appendChild(page);
    http.expectOne(URL_API).flush(identite);
    fixture.detectChanges();
  };

  const champ = <T extends HTMLElement = HTMLInputElement>(id: string): T => page.querySelector<T>(`#${id}`)!;

  const saisir = (id: string, valeur: string): void => {
    const element = champ(id);
    element.value = valeur;
    element.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  const choisirListe = (id: string, valeur: string): void => {
    const liste = champ<HTMLSelectElement>(id);
    liste.value = valeur;
    liste.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  const deposer = async (id: string, fichier: File): Promise<void> => {
    const element = champ(id);
    Object.defineProperty(element, 'files', { value: [fichier], configurable: true });
    element.dispatchEvent(new Event('change'));
    await new Promise((resolve) => setTimeout(resolve));
    await rafraichir();
  };

  const bouton = (texte: string): HTMLButtonElement =>
    Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === texte)!;

  const remplirIdentite = (): void => {
    bouton('Madame').click();
    fixture.detectChanges();
    saisir('nom', 'Durand');
    saisir('prenom', 'Marie');
    saisir('date-naissance', '31011990');
    saisir('telephone', '06 12 34 56 78');
  };

  const remplirTout = async (): Promise<void> => {
    remplirIdentite();
    choisirListe('type-identite', 'passeport');
    choisirListe('type-domicile', 'quittance-loyer');
    await deposer('fichier-identite', vraiPdf());
    await deposer('fichier-domicile', vraiPng());
  };

  const valider = (): void => {
    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  };

  const erreurs = (): string[] =>
    Array.from(page.querySelectorAll('.erreur')).map((e) => e.textContent?.trim() ?? '');

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    devis = TestBed.inject(DevisService);
    devis.validerRecommandation('ref-1', RECO_CONFORT);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    page.remove();
  });

  describe('AC1 : identité', () => {
    it('laisse tout vide, sans civilité présélectionnée, pour un compte sans identité', () => {
      creerEcran({ civilite: null, nom: null, prenom: null, dateNaissance: null, telephone: null });

      expect(page.querySelector('[aria-pressed="true"]')).toBeNull();
      for (const id of ['nom', 'prenom', 'date-naissance', 'telephone']) {
        expect(champ(id).value).toBe('');
      }
    });

    it('reste utilisable si le compte ne peut pas être lu', () => {
      fixture = TestBed.createComponent(DevisInformations);
      fixture.detectChanges();
      page = fixture.nativeElement as HTMLElement;
      http.expectOne(URL_API).flush(null, { status: 500, statusText: 'Erreur' });
      fixture.detectChanges();

      expect(champ('nom').value).toBe('');
    });

    it('pré-remplit avec ce que le client a déjà saisi', () => {
      creerEcran({
        civilite: 'monsieur',
        nom: 'Durand',
        prenom: 'Paul',
        dateNaissance: '1990-01-31',
        telephone: '0612345678',
      });

      expect(bouton('Monsieur').getAttribute('aria-pressed')).toBe('true');
      expect(champ('nom').value).toBe('Durand');
      expect(champ('prenom').value).toBe('Paul');
      expect(champ('date-naissance').value).toBe('31/01/1990');
      expect(champ('telephone').value).toBe('0612345678');
    });

    it('n’écrase pas une saisie déjà faite', () => {
      devis.informations.update((s) => ({ ...s, nom: 'Martin' }));
      creerEcran({ nom: 'Durand', prenom: 'Paul' });

      expect(champ('nom').value).toBe('Martin');
      expect(champ('prenom').value).toBe('Paul');
    });

    it('ajoute les barres de la date pendant la frappe', () => {
      creerEcran();
      saisir('date-naissance', '31011990');
      expect(champ('date-naissance').value).toBe('31/01/1990');
    });
  });

  describe('AC2 : zones de dépôt', () => {
    beforeEach(() => creerEcran());

    it('propose deux zones et leur liste, sans valeur présélectionnée', () => {
      expect(page.querySelector('#fichier-identite')).not.toBeNull();
      expect(page.querySelector('#fichier-domicile')).not.toBeNull();
      expect(champ<HTMLSelectElement>('type-identite').value).toBe('');
      expect(champ<HTMLSelectElement>('type-domicile').value).toBe('');
    });

    it('RG3 : propose les bons types de document', () => {
      const libelles = (id: string): string[] =>
        Array.from(champ<HTMLSelectElement>(id).options).map((o) => o.textContent?.trim() ?? '');

      expect(libelles('type-identite')).toEqual([
        'Choisissez',
        "Carte d'identité",
        'Passeport',
        'Titre de séjour',
      ]);
      expect(libelles('type-domicile')).toEqual([
        'Choisissez',
        "Facture d'électricité",
        'Facture de gaz',
        "Facture d'internet",
        'Quittance de loyer',
      ]);
    });

    it('n’impose pas l’appareil photo, pour laisser le choix sur téléphone', () => {
      expect(champ('fichier-identite').hasAttribute('capture')).toBe(false);
    });

    it('affiche le nom du fichier, une coche verte et le lien Remplacer', async () => {
      await deposer('fichier-identite', vraiPdf());

      expect(page.querySelector('.depot__nom')?.textContent).toBe('carte.pdf');
      expect(page.querySelector('.depot--accepte .depot__coche')).not.toBeNull();
      expect(bouton('Remplacer')).toBeDefined();
    });

    it('Remplacer rouvre le choix de fichier, et le nouveau prend la place de l’ancien', async () => {
      await deposer('fichier-identite', vraiPdf());
      const ouverture = vi.spyOn(champ('fichier-identite'), 'click');

      bouton('Remplacer').click();
      expect(ouverture).toHaveBeenCalled();

      await deposer('fichier-identite', vraiPng('nouvelle.png'));
      expect(page.querySelector('.depot__nom')?.textContent).toBe('nouvelle.png');
    });

    it('accepte un fichier glissé dans la zone', async () => {
      const zone = page.querySelector('.depot')!;
      const survol = new Event('dragover', { cancelable: true });
      zone.dispatchEvent(survol);
      expect(survol.defaultPrevented).toBe(true);

      const lacher = new Event('drop', { cancelable: true });
      Object.defineProperty(lacher, 'dataTransfer', { value: { files: [vraiPdf('glisse.pdf')] } });
      zone.dispatchEvent(lacher);
      await new Promise((resolve) => setTimeout(resolve));
      await rafraichir();

      expect(lacher.defaultPrevented).toBe(true);
      expect(page.querySelector('.depot__nom')?.textContent).toBe('glisse.pdf');
    });

    it('ignore un glisser-déposer sans fichier', async () => {
      const lacher = new Event('drop', { cancelable: true });
      Object.defineProperty(lacher, 'dataTransfer', { value: { files: [] } });
      page.querySelector('.depot')!.dispatchEvent(lacher);
      await rafraichir();

      expect(page.querySelector('.depot__nom')).toBeNull();
      expect(erreurs()).toEqual([]);
    });

    it('ignore une fenêtre de choix fermée sans fichier', async () => {
      const element = champ('fichier-identite');
      Object.defineProperty(element, 'files', { value: [], configurable: true });
      element.dispatchEvent(new Event('change'));
      await rafraichir();

      expect(page.querySelector('.depot__nom')).toBeNull();
    });
  });

  describe('AC3 : fichier refusé', () => {
    beforeEach(() => creerEcran());

    it('explique le format refusé sous la zone concernée, dès le dépôt', async () => {
      await deposer('fichier-domicile', new File(['texte'], 'faux.pdf'));

      const message = page.querySelector('#erreur-fichier-domicile')?.textContent;
      expect(message).toContain("Ce format n'est pas accepté");
      expect(message).toContain('PDF, JPG ou PNG');
      expect(page.querySelector('#erreur-fichier-identite')).toBeNull();
      expect(page.querySelector('.depot__nom')).toBeNull();
    });

    it('explique le dépassement de taille', async () => {
      const contenu = new Uint8Array(5 * 1024 * 1024 + 1);
      contenu.set(PDF);
      await deposer('fichier-identite', new File([contenu], 'gros.pdf'));

      expect(page.querySelector('#erreur-fichier-identite')?.textContent).toContain('dépasse 5 Mo');
    });

    it('retire l’ancien fichier si son remplaçant est refusé, puis efface le message au bon dépôt', async () => {
      await deposer('fichier-identite', vraiPdf());
      await deposer('fichier-identite', new File(['texte'], 'faux.pdf'));
      expect(page.querySelector('.depot__nom')).toBeNull();

      await deposer('fichier-identite', vraiPdf());
      expect(page.querySelector('#erreur-fichier-identite')).toBeNull();
    });

    it('bloque « Continuer » tant que le fichier refusé n’est pas remplacé', async () => {
      await remplirTout();
      await deposer('fichier-identite', new File(['texte'], 'faux.pdf'));

      valider();

      http.expectNone(URL_API);
      expect(document.activeElement).toBe(champ('fichier-identite'));
    });
  });

  describe('AC4 : colocation', () => {
    it('demande un nom par colocataire, moins le souscripteur', () => {
      devis.mettreAJour({ statut: 'colocation', nombreColocataires: '4' });
      creerEcran();

      expect(page.querySelectorAll('[id^="colocataire-"]')).toHaveLength(3);
    });

    it('ne demande rien hors colocation', () => {
      devis.mettreAJour({ statut: 'locataire', nombreColocataires: '4' });
      creerEcran();

      expect(page.textContent).not.toContain('Mes colocataires');
    });

    it('exige chaque nom et les envoie', async () => {
      devis.mettreAJour({ statut: 'colocation', nombreColocataires: '3' });
      creerEcran();
      await remplirTout();
      saisir('colocataire-0', 'Paul Martin');

      valider();
      http.expectNone(URL_API);
      expect(erreurs()).toEqual(['Indiquez le nom et le prénom de ce colocataire.']);
      expect(document.activeElement).toBe(champ('colocataire-1'));

      saisir('colocataire-1', 'Léa Petit');
      valider();
      const requete = http.expectOne((r) => r.method === 'POST');
      const informations = JSON.parse(await ((requete.request.body as FormData).get('informations') as Blob).text());
      expect(informations.colocataires).toEqual(['Paul Martin', 'Léa Petit']);
      requete.flush(null, { status: 204, statusText: 'No Content' });
    });
  });

  describe('AC5 : « Continuer vers le paiement »', () => {
    beforeEach(() => creerEcran());

    it('n’affiche aucune erreur avant le clic', () => {
      expect(erreurs()).toEqual([]);
    });

    it('reste sur l’étape, affiche chaque erreur et place le curseur sur la première', () => {
      valider();

      expect(router.navigate).not.toHaveBeenCalled();
      expect(erreurs()).toEqual([
        'Indiquez votre civilité.',
        'Indiquez votre nom.',
        'Indiquez votre prénom.',
        'Indiquez votre date de naissance.',
        'Indiquez votre numéro de téléphone.',
        'Choisissez le type de document.',
        "Déposez votre pièce d'identité.",
        'Choisissez le type de document.',
        'Déposez votre justificatif de domicile.',
      ]);
      expect(document.activeElement).toBe(champ('civilite'));
    });

    it('refuse un client mineur', async () => {
      await remplirTout();
      saisir('date-naissance', '01012020');

      valider();

      expect(erreurs()).toEqual(['Vous devez avoir au moins 18 ans pour souscrire.']);
      expect(document.activeElement).toBe(champ('date-naissance'));
      http.expectNone(URL_API);
    });

    it('refuse un téléphone invalide', async () => {
      await remplirTout();
      saisir('telephone', '123');

      valider();

      expect(erreurs()[0]).toContain('10 chiffres');
      expect(document.activeElement).toBe(champ('telephone'));
    });

    it('refuse un justificatif manquant et y place le curseur', async () => {
      remplirIdentite();
      choisirListe('type-identite', 'passeport');
      choisirListe('type-domicile', 'gaz');
      await deposer('fichier-identite', vraiPdf());

      valider();

      expect(erreurs()).toEqual(['Déposez votre justificatif de domicile.']);
      expect(document.activeElement).toBe(champ('fichier-domicile'));
    });

    it('efface l’erreur dès que le champ est corrigé', () => {
      valider();
      saisir('nom', 'Durand');
      expect(erreurs()).not.toContain('Indiquez votre nom.');
    });
  });

  describe('enregistrement', () => {
    beforeEach(() => creerEcran());

    it('envoie les informations et passe à l’étape suivante', async () => {
      await remplirTout();
      valider();

      expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(true);
      const requete = http.expectOne((r) => r.method === 'POST');
      const corps = requete.request.body as FormData;
      expect(JSON.parse(await (corps.get('informations') as Blob).text())).toEqual({
        referenceDevis: 'ref-1',
        civilite: 'madame',
        nom: 'Durand',
        prenom: 'Marie',
        dateNaissance: '1990-01-31',
        telephone: '0612345678',
        typePieceIdentite: 'passeport',
        typeJustificatifDomicile: 'quittance-loyer',
        colocataires: [],
      });
      expect((corps.get('pieceIdentite') as File).name).toBe('carte.pdf');
      expect((corps.get('justificatifDomicile') as File).name).toBe('quittance.png');

      requete.flush(null, { status: 204, statusText: 'No Content' });
      expect(router.navigate).toHaveBeenCalledWith(['/devis/paiement']);
      expect(devis.referenceDossier()).toBe('ref-1');
    });

    it('n’envoie pas deux fois pendant l’enregistrement', async () => {
      await remplirTout();
      valider();
      valider();

      http.expectOne((r) => r.method === 'POST').flush(null, { status: 204, statusText: 'No Content' });
    });

    it('affiche sous la zone un fichier refusé par le serveur', async () => {
      await remplirTout();
      valider();

      http
        .expectOne((r) => r.method === 'POST')
        .flush({ champ: 'justificatifDomicile', detail: 'Refusé.' }, { status: 422, statusText: 'Unprocessable' });
      await rafraichir();

      expect(page.querySelector('#erreur-fichier-domicile')?.textContent).toBe('Refusé.');
      expect(page.querySelectorAll('.depot__nom')).toHaveLength(1);
      expect(document.activeElement).toBe(champ('fichier-domicile'));
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('affiche un message général pour un autre refus du serveur', async () => {
      await remplirTout();
      valider();

      http
        .expectOne((r) => r.method === 'POST')
        .flush({ champ: 'dateNaissance', detail: 'Refusé.' }, { status: 422, statusText: 'Unprocessable' });
      await rafraichir();

      expect(page.querySelector('.alerte')?.textContent).toContain("n'ont pas pu être enregistrées");
    });

    it('reste sur l’écran en cas d’échec, et permet de réessayer', async () => {
      await remplirTout();
      valider();

      http.expectOne((r) => r.method === 'POST').flush(null, { status: 500, statusText: 'Erreur' });
      await rafraichir();

      expect(page.querySelector('.alerte')).not.toBeNull();
      expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(false);
      expect(router.navigate).not.toHaveBeenCalled();

      valider();
      expect(page.querySelector('.alerte')).toBeNull();
      http.expectOne((r) => r.method === 'POST').flush(null, { status: 204, statusText: 'No Content' });
    });

    it('renvoie à la connexion si la session a expiré, en retenant l’étape', async () => {
      await remplirTout();
      valider();

      http.expectOne((r) => r.method === 'POST').flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(router.navigate).toHaveBeenCalledWith(['/connexion'], {
        queryParams: { retour: '/devis/informations' },
      });
    });
  });

  it('RG5 : affiche la mention RGPD sous les zones de dépôt', () => {
    creerEcran();
    const mention = page.querySelector('.mention-legale')!;

    expect(mention.textContent).toContain('uniquement pour vérifier votre identité et l\'adresse assurée');
    const zones = page.querySelectorAll('.depot');
    expect(zones[zones.length - 1].compareDocumentPosition(mention)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });

  it('conserve la saisie et les fichiers au retour sur l’étape', async () => {
    creerEcran();
    await remplirTout();
    page.remove();

    creerEcran();

    expect(champ('nom').value).toBe('Durand');
    expect(champ<HTMLSelectElement>('type-identite').value).toBe('passeport');
    expect(page.querySelectorAll('.depot__nom')).toHaveLength(2);
  });

  it('affiche l’étape en cours et le retour à la formule', () => {
    creerEcran();

    expect(page.querySelector('h1')?.textContent).toContain('Mes informations');
    expect(page.querySelector('[aria-current="step"]')?.textContent).toContain('Mes informations');
    const lien = Array.from(page.querySelectorAll('a')).find((a) => a.textContent?.includes('Retour'));
    expect(lien?.getAttribute('href')).toBe('/devis/formule');
  });
});
