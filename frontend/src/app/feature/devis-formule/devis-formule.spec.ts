import { RECO_CONFORT, tarifDetaille } from '../../core/model/tarif.exemples';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { DevisService } from '../../core/service/devis.service';
import { DevisFormule } from './devis-formule';

describe('DevisFormule', () => {
  let fixture: ComponentFixture<DevisFormule>;
  let page: HTMLElement;
  let devis: DevisService;
  let router: Router;

  const creerEcran = (): void => {
    fixture = TestBed.createComponent(DevisFormule);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };

  const synchroniser = (): void => {
    vi.advanceTimersByTime(300);
    TestBed.inject(HttpTestingController)
      .match('/api/assurance-iard-api/devis/ref-1/tarif')
      .forEach((requete) =>
        requete.flush(tarifDetaille(devis.formuleChoisie() ?? 'confort', devis.optionsChoisies(), devis.franchiseChoisie())),
      );
    fixture.detectChanges();
  };

  const carteChoisie = (): string => page.querySelector('.formule--choisie .formule__nom')?.textContent ?? '';

  const cliquer = (texte: string): void => {
    const bouton = Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === texte);
    bouton!.click();
    synchroniser();
  };

  const choisirFranchise = (valeur: string): void => {
    const radio = page.querySelector<HTMLInputElement>(`input[type="radio"][value="${valeur}"]`)!;
    radio.click();
    synchroniser();
  };

  const cocherOption = (libelle: string): void => {
    const labels = Array.from(page.querySelectorAll('label.option'));
    const label = labels.find((l) => l.textContent?.includes(libelle));
    const checkbox = label?.querySelector<HTMLInputElement>('input[type="checkbox"]')!;
    checkbox.click();
    synchroniser();
  };

  const soumettre = (): void => {
    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  };

  const recapitulatif = (): string => page.querySelector('.recapitulatif')?.textContent ?? '';

  afterEach(() => {
    vi.useRealTimers();
  });

  beforeEach(() => {
    sessionStorage.clear();
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    devis = TestBed.inject(DevisService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    devis.mettreAJour({ surface: '60', typeLogement: 'appartement' });
    devis.mettreAJourBesoins({
      valeurContenu: 15000,
      objetsValeur: true,
      appareilsCouteux: false,
      remboursement: 'vetuste',
      assistance: true,
      litiges: false,
      priorite: 'prix',
    });
    devis.validerRecommandation('ref-1', RECO_CONFORT);
    devis.tarif.set(tarifDetaille());
    creerEcran();
  });

  it('présente les trois formules avec leur prix', () => {
    const formules = Array.from(page.querySelectorAll('.formule')).map((f) => f.textContent);
    expect(formules).toHaveLength(3);
    expect(formules[0]).toContain('Essentielle');
    expect(formules[0]).toContain('9,40');
    expect(formules[1]).toContain('Confort');
    expect(formules[1]).toContain('12,22');
    expect(formules[2]).toContain('Sérénité');
    expect(formules[2]).toContain('15,04');
  });

  it('affiche la liste des garanties dans chaque carte', () => {
    const cartes = page.querySelectorAll('.formule');
    expect(cartes[0].textContent).toContain('Responsabilité civile');
    expect(cartes[1].textContent).toContain('Bris de glace');
    expect(cartes[2].textContent).toContain('Remboursement à neuf');
  });

  it('AC3 : la formule conseillée est sélectionnée', () => {
    expect(carteChoisie()).toBe('Confort');
    expect(page.querySelectorAll('[aria-pressed="true"]')).toHaveLength(1);
    expect(page.querySelectorAll('.formule__badge')).toHaveLength(1);
    expect(page.querySelector('.formule--choisie .formule__badge')?.textContent).toBe('Conseillée');
  });

  it('AC3 : rappelle les raisons', () => {
    expect(page.textContent).toContain('Pourquoi la formule Confort ?');
    expect(page.textContent).toContain('Vous déclarez des objets de valeur');
  });

  it('RG3 : rappelle les options conseillées', () => {
    const section = page.querySelector('[aria-labelledby="titre-raisons"]');
    expect(section?.textContent).toContain('Objets de valeur');
    expect(section?.textContent).toContain('Assistance dépannage');
    expect(section?.textContent).not.toContain('Protection juridique');
  });

  it("indique quand aucune option n'est conseillée", () => {
    devis.mettreAJourBesoins({ objetsValeur: false, assistance: false });
    fixture.detectChanges();
    expect(page.textContent).toContain('Aucune option conseillée');
  });

  it('permet de choisir une autre formule, le conseil restant affiché', () => {
    page.querySelector<HTMLButtonElement>('[aria-label="Choisir la formule Sérénité"]')!.click();
    fixture.detectChanges();

    expect(carteChoisie()).toBe('Sérénité');
    expect(devis.formuleChoisie()).toBe('serenite');
    expect(page.querySelector('.sous-titre')?.textContent).toContain('Confort');
  });

  it('propose le retour aux besoins', () => {
    const lien = Array.from(page.querySelectorAll('a')).find((a) => a.textContent?.includes('Retour'));
    expect(lien?.getAttribute('href')).toBe('/devis/besoins');
  });

  describe('Options', () => {
    it("affiche 3 options sans aucune cochée par défaut", () => {
      const checkboxes = page.querySelectorAll<HTMLInputElement>('input[type="checkbox"]');
      expect(checkboxes).toHaveLength(3);
      checkboxes.forEach((cb) => expect(cb.checked).toBe(false));
      expect(page.textContent).toContain('Objets de valeur');
      expect(page.textContent).toContain('Assistance dépannage');
      expect(page.textContent).toContain('Protection juridique');
    });

    it('coche une option et la fait apparaître dans le récapitulatif', () => {
      cocherOption('Objets de valeur');
      expect(devis.optionsChoisies()).toEqual(['objets-valeur']);
      expect(recapitulatif()).toContain('Objets de valeur');
      expect(recapitulatif()).toContain('+2,50');
    });

    it("décoche une option déjà cochée", () => {
      cocherOption('Objets de valeur');
      cocherOption('Objets de valeur');
      expect(devis.optionsChoisies()).toEqual([]);
      expect(recapitulatif()).not.toContain('Objets de valeur');
    });
  });

  describe('Franchise', () => {
    it("affiche 3 choix de franchise sans aucun sélectionné par défaut", () => {
      const radios = page.querySelectorAll<HTMLInputElement>('input[type="radio"]');
      expect(radios).toHaveLength(3);
      radios.forEach((r) => expect(r.checked).toBe(false));
      expect(page.textContent).toContain('Franchise 150');
      expect(page.textContent).toContain('Franchise 300');
      expect(page.textContent).toContain('Franchise 500');
      expect(page.textContent).toContain('La franchise catastrophe naturelle est fixée par la loi');
    });

    it('sélectionne une franchise et la fait apparaître dans le récapitulatif', () => {
      choisirFranchise('150');
      expect(devis.franchiseChoisie()).toBe('150');
      expect(recapitulatif()).toContain('Franchise 150');
      expect(recapitulatif()).toContain('+1,50');
    });

    it("affiche un ajustement négatif pour la franchise 500 €", () => {
      choisirFranchise('500');
      expect(recapitulatif()).toContain('-1,00');
    });

    it("affiche « prix de base » pour la franchise 300 €", () => {
      expect(page.textContent).toContain('prix de base');
    });
  });

  describe('Récapitulatif Mon devis', () => {
    it("affiche la formule dès l'arrivée (sélectionnée automatiquement)", () => {
      expect(recapitulatif()).toContain('Formule Confort');
      expect(recapitulatif()).toContain('12,22');
    });

    it('calcule le total avec options et franchise', () => {
      cocherOption('Objets de valeur');
      choisirFranchise('150');
      // 12.22 + 2.50 + 1.50 = 16.22
      expect(recapitulatif()).toContain('Total TTC');
      expect(recapitulatif()).toContain('16,22');
    });
  });

  describe('Sélecteur mensuel / annuel', () => {
    it('affiche les prix mensuels par défaut', () => {
      expect(page.textContent).toContain('/mois');
      expect(page.querySelector('.toggle-periodicite__bouton--actif')?.textContent?.trim()).toBe('Par mois');
    });

    it('multiplie tous les prix par 12 en mode annuel', () => {
      cliquer('Par an');
      // Confort mensuel = 12.22 → annuel = 12.22 * 12 = 146.64
      const formules = Array.from(page.querySelectorAll('.formule')).map((f) => f.textContent);
      expect(formules[1]).toContain('146,64');
      expect(recapitulatif()).toContain('146,64');
    });

    it("affiche l'équivalent mensuel sous le total en mode annuel", () => {
      choisirFranchise('300');
      cliquer('Par an');
      expect(page.querySelector('.recapitulatif__mensuel')?.textContent).toContain('/mois');
    });

    it('repasse en mode mensuel', () => {
      cliquer('Par an');
      cliquer('Par mois');
      expect(page.textContent).toContain('12,22');
      expect(page.querySelector('.recapitulatif__mensuel')).toBeNull();
    });
  });

  describe("Alerte formule inférieure", () => {
    it("masque l'alerte quand la formule conseillée est choisie", () => {
      expect(page.querySelector('[role="alert"]')).toBeNull();
    });

    it("affiche l'alerte quand on choisit une formule inférieure à la recommandation", () => {
      page.querySelector<HTMLButtonElement>('[aria-label="Choisir la formule Essentielle"]')!.click();
      fixture.detectChanges();
      expect(page.querySelector('[role="alert"]')?.textContent).toContain('couvre moins que nos conseils');
    });

    it("masque l'alerte quand on revient à la formule recommandée", () => {
      page.querySelector<HTMLButtonElement>('[aria-label="Choisir la formule Essentielle"]')!.click();
      fixture.detectChanges();
      page.querySelector<HTMLButtonElement>('[aria-label="Choisir la formule Confort"]')!.click();
      fixture.detectChanges();
      expect(page.querySelector('[role="alert"]')).toBeNull();
    });
  });

  describe('Alerte capital insuffisant', () => {
    it("masque l'alerte si pas de valeur de contenu", () => {
      devis.mettreAJourBesoins({ valeurContenu: null });
      fixture.detectChanges();
      expect(page.querySelectorAll('[role="alert"]')).toHaveLength(0);
    });

    it("affiche l'alerte quand le contenu dépasse le capital de la formule choisie", () => {
      // 40 000 > plafond Sérénité (35 000), formule conseillée = serenite (pas d'alerte inférieure)
      devis.mettreAJourBesoins({ valeurContenu: 40000 });
      devis.validerRecommandation('ref-2', { ...RECO_CONFORT, formule: 'serenite' });
      fixture.detectChanges();
      const alertes = Array.from(page.querySelectorAll('[role="alert"]'));
      expect(alertes.some((a) => a.textContent?.includes('insuffisant'))).toBe(true);
    });
  });

  describe('Boutons du récapitulatif', () => {
    it('affiche le bouton Enregistrer mon devis dans le bloc Mon devis', () => {
      const btn = Array.from(page.querySelectorAll('.recapitulatif button')).find((b) =>
        b.textContent?.includes('Enregistrer'),
      );
      expect(btn).not.toBeNull();
    });

    it("Enregistrer mon devis : affiche l'erreur franchise si aucune franchise n'est choisie", () => {
      const btn = Array.from(page.querySelectorAll<HTMLButtonElement>('.recapitulatif button')).find((b) =>
        b.textContent?.includes('Enregistrer'),
      )!;
      btn.click();
      fixture.detectChanges();
      expect(page.querySelector('#erreur-franchise')).not.toBeNull();
      expect(router.navigate).not.toHaveBeenCalled();
    });

    describe('US-12 : enregistrer et télécharger', () => {
      const JETON = 'assureasy.session';
      const remplir = (): void => {
        devis.mettreAJour({ adresse: '12 rue des Lilas', codePostal: '75001', ville: 'Paris' });
        choisirFranchise('300');
      };
      const flushSession = (http: HttpTestingController): void => {
        http.expectOne('/api/assurance-iard-api/session').flush({ email: 'marie@exemple.fr' });
      };

      afterEach(() => {
        TestBed.inject(HttpTestingController).verify();
        sessionStorage.clear();
      });

      it('AC1 : connecté, enregistre et confirme avec la date de validité', () => {
        const http = TestBed.inject(HttpTestingController);
        sessionStorage.setItem(JETON, 'jeton');
        remplir();
        cliquer('Enregistrer mon devis');
        flushSession(http);
        const requete = http.expectOne('/api/assurance-iard-api/devis/enregistres');
        expect(requete.request.body.franchise).toBe('300');
        expect(requete.request.body.prixMensuel).toBeUndefined();
        requete.flush({ dateFinValidite: '2026-12-05' });
        fixture.detectChanges();
        expect(page.querySelector('[role="status"]')?.textContent).toContain("valable jusqu'au 05/12/2026");
        expect(page.querySelector('a[href="/espace-client/devis"]')).not.toBeNull();
      });

      it('AC2 : non connecté, propose de créer un compte ou de se connecter sans rien envoyer', () => {
        const http = TestBed.inject(HttpTestingController);
        remplir();
        cliquer('Enregistrer mon devis');
        http.expectNone('/api/assurance-iard-api/devis/enregistres');
        const liens = Array.from(page.querySelectorAll<HTMLAnchorElement>('.encadre-compte a')).map((a) =>
          a.getAttribute('href'),
        );
        expect(liens[0]).toContain('/inscription?retour=');
        expect(liens[1]).toContain('/connexion?retour=');
        expect(decodeURIComponent(liens[1] ?? '')).toContain('/devis/formule?enregistrer=1');
        expect(sessionStorage.getItem('assureasy.devis')).toContain('12 rue des Lilas');
      });

      it('AC2 : une session expirée propose aussi le compte', () => {
        const http = TestBed.inject(HttpTestingController);
        sessionStorage.setItem(JETON, 'jeton');
        remplir();
        cliquer('Enregistrer mon devis');
        flushSession(http);
        http.expectOne('/api/assurance-iard-api/devis/enregistres').flush(null, { status: 401, statusText: 'Unauthorized' });
        fixture.detectChanges();
        expect(page.querySelector('.encadre-compte')).not.toBeNull();
      });

      it('AC2 : au retour de la connexion, enregistre seul et efface la copie', () => {
        const http = TestBed.inject(HttpTestingController);
        remplir();
        devis.conserverBrouillon();
        fixture.destroy();
        vi.spyOn(TestBed.inject(ActivatedRoute).snapshot.queryParamMap, 'get').mockReturnValue('1');
        creerEcran();
        http.expectOne('/api/assurance-iard-api/devis/enregistres').flush({ dateFinValidite: '2026-12-05' });
        fixture.detectChanges();
        expect(page.textContent).toContain('Votre devis est enregistré');
        expect(router.navigate).toHaveBeenCalledWith(['/devis/formule'], { replaceUrl: true });
        expect(sessionStorage.getItem('assureasy.devis')).toBeNull();
      });

      it('signale un échec de l’enregistrement', () => {
        const http = TestBed.inject(HttpTestingController);
        sessionStorage.setItem(JETON, 'jeton');
        remplir();
        cliquer('Enregistrer mon devis');
        flushSession(http);
        http.expectOne('/api/assurance-iard-api/devis/enregistres').flush(null, { status: 500, statusText: 'Erreur' });
        fixture.detectChanges();
        expect(page.querySelector('.recapitulatif [role="alert"]')?.textContent).toContain('pas pu être enregistré');
      });

      it('retire la confirmation dès qu’un choix change', () => {
        const http = TestBed.inject(HttpTestingController);
        sessionStorage.setItem(JETON, 'jeton');
        remplir();
        cliquer('Enregistrer mon devis');
        flushSession(http);
        http.expectOne('/api/assurance-iard-api/devis/enregistres').flush({ dateFinValidite: '2026-12-05' });
        fixture.detectChanges();
        choisirFranchise('500');
        expect(page.textContent).not.toContain('Votre devis est enregistré');
      });

      it('AC6 : affiche le message de reprise', () => {
        devis.messageReprise.set('Votre devis avait expiré.');
        fixture.detectChanges();
        expect(page.querySelector('.message-reprise')?.textContent).toContain('avait expiré');
      });

      it('informe de la durée de conservation', () => {
        expect(page.querySelector('.recapitulatif__mention')?.textContent).toContain('conservé 3 ans');
      });

      it('AC4 : sans compte, télécharge un PDF', () => {
        const http = TestBed.inject(HttpTestingController);
        remplir();
        const fakeA = { href: '', download: '', click: vi.fn() } as unknown as HTMLAnchorElement;
        URL.createObjectURL = vi.fn().mockReturnValue('blob:devis');
        URL.revokeObjectURL = vi.fn();
        cliquer('Télécharger mon devis (PDF)');
        vi.spyOn(document, 'createElement').mockReturnValueOnce(fakeA);
        http.expectOne('/api/assurance-iard-api/devis/pdf').flush(new Blob(['%PDF']));
        expect(fakeA.download).toBe('devis-assureasy.pdf');
        expect(fakeA.click).toHaveBeenCalled();
        expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:devis');
      });

      it('AC4 : demande la franchise avant de télécharger', () => {
        cliquer('Télécharger mon devis (PDF)');
        expect(page.querySelector('#erreur-franchise')).not.toBeNull();
        TestBed.inject(HttpTestingController).expectNone('/api/assurance-iard-api/devis/pdf');
      });

      it('signale un PDF impossible à générer', () => {
        const http = TestBed.inject(HttpTestingController);
        remplir();
        cliquer('Télécharger mon devis (PDF)');
        http.expectOne('/api/assurance-iard-api/devis/pdf').flush(new Blob(), { status: 500, statusText: 'Erreur' });
        fixture.detectChanges();
        expect(page.querySelector('.recapitulatif [role="alert"]')?.textContent).toContain('pas pu être généré');
      });
    });

    it('affiche le bouton Télécharger mon devis (PDF) dans le bloc Mon devis', () => {
      const btn = Array.from(page.querySelectorAll('.recapitulatif button')).find((b) =>
        b.textContent?.includes('Télécharger'),
      );
      expect(btn).not.toBeNull();
    });

  });

  describe('Bouton Souscrire cette formule', () => {
    it("affiche l'erreur franchise si aucune franchise n'est choisie", () => {
      soumettre();
      expect(page.querySelector('#erreur-franchise')?.textContent).toContain('Choisissez votre franchise');
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it("ne navigue pas tant que la franchise n'est pas choisie", () => {
      soumettre();
      soumettre();
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('navigue vers /devis/informations quand la franchise est choisie', () => {
      choisirFranchise('300');
      soumettre();
      expect(router.navigate).toHaveBeenCalledWith(['/devis/informations']);
    });

    it("masque l'erreur franchise après avoir choisi une franchise", () => {
      soumettre();
      expect(page.querySelector('#erreur-franchise')).not.toBeNull();
      choisirFranchise('500');
      expect(page.querySelector('#erreur-franchise')).toBeNull();
    });
  });

  describe('Tarif du serveur', () => {
    const URL = '/api/assurance-iard-api/devis/ref-1/tarif';
    const requetes = (): HttpTestingController => TestBed.inject(HttpTestingController);

    afterEach(() => requetes().verify());

    it('n’appelle pas le serveur à l’arrivée si le prix est déjà connu', () => {
      requetes().expectNone(URL);
    });

    it('envoie les choix du client', () => {
      page.querySelector<HTMLInputElement>('input[type="radio"][value="500"]')!.click();
      vi.advanceTimersByTime(300);
      const requete = requetes().expectOne(URL);
      expect(requete.request.body.formule).toBe('confort');
      expect(requete.request.body.franchise).toBe('500');
      requete.flush(tarifDetaille('confort', [], '500'));
    });

    it('plusieurs clics rapides ne font qu’un seul calcul', () => {
      page.querySelector<HTMLInputElement>('input[type="radio"][value="500"]')!.click();
      vi.advanceTimersByTime(100);
      page.querySelector<HTMLInputElement>('input[type="radio"][value="150"]')!.click();
      vi.advanceTimersByTime(300);
      expect(requetes().expectOne(URL).request.body.franchise).toBe('150');
    });

    it('bloque la souscription tant que le calcul est en cours', () => {
      choisirFranchise('300');
      page.querySelector<HTMLInputElement>('input[type="radio"][value="500"]')!.click();
      fixture.detectChanges();
      const souscrire = page.querySelector<HTMLButtonElement>('button[type="submit"]')!;
      expect(souscrire.disabled).toBe(true);
      synchroniser();
      expect(souscrire.disabled).toBe(false);
    });

    it('affiche un message et aucun total si le calcul échoue', () => {
      page.querySelector<HTMLInputElement>('input[type="radio"][value="500"]')!.click();
      vi.advanceTimersByTime(300);
      requetes().expectOne(URL).flush(null, { status: 503, statusText: 'Indisponible' });
      fixture.detectChanges();
      expect(recapitulatif()).toContain("n'a pas pu être calculé");
      expect(recapitulatif()).not.toContain('Total TTC');
      soumettre();
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('affiche le motif si le moteur refuse', () => {
      page.querySelector<HTMLInputElement>('input[type="radio"][value="500"]')!.click();
      vi.advanceTimersByTime(300);
      requetes().expectOne(URL).flush({ idTarif: 'x', tarifable: false, motif: 'Motif de refus' });
      fixture.detectChanges();
      expect(recapitulatif()).toContain('Motif de refus');
    });

    it('à l’arrivée depuis l’étape 2, calcule le détail du prix', () => {
      devis.tarif.set({ ...tarifDetaille(), detail: undefined });
      fixture.destroy();
      creerEcran();
      const requete = requetes().expectOne(URL);
      expect(requete.request.method).toBe('POST');
      requete.flush(tarifDetaille());
      fixture.detectChanges();
      expect(recapitulatif()).toContain('Total TTC');
    });

    it('un devis repris retrouve son tarif d’origine', () => {
      devis.tarif.set(null);
      fixture.destroy();
      creerEcran();
      const relecture = requetes().expectOne(URL);
      expect(relecture.request.method).toBe('GET');
      relecture.flush(tarifDetaille());
      fixture.detectChanges();
      expect(devis.recommandation()).toEqual(RECO_CONFORT);
      expect(recapitulatif()).toContain('12,22');
    });

    it('un devis repris dont le tarif a expiré obtient un nouveau calcul', () => {
      devis.tarif.set(null);
      fixture.destroy();
      creerEcran();
      requetes().expectOne(URL).flush(null, { status: 410, statusText: 'Expiré' });
      const calcul = requetes().expectOne(URL);
      expect(calcul.request.method).toBe('POST');
      calcul.flush(tarifDetaille());
    });
  });
});
