import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { DevisService } from '../../core/service/devis.service';
import { DevisBesoins } from './devis-besoins';

describe('DevisBesoins', () => {
  let fixture: ComponentFixture<DevisBesoins>;
  let page: HTMLElement;
  let devis: DevisService;
  let router: Router;
  let http: HttpTestingController;

  const creerEcran = (): void => {
    fixture = TestBed.createComponent(DevisBesoins);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };

  const repondre = (question: number, texte: string): void => {
    const groupe = page.querySelectorAll('[role="group"]')[question];
    const bouton = Array.from(groupe.querySelectorAll('button')).find((b) => b.textContent?.trim() === texte);
    bouton!.click();
    fixture.detectChanges();
  };

  const saisirContenu = (valeur: string): HTMLInputElement => {
    const champ = page.querySelector<HTMLInputElement>('#valeur-contenu')!;
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    return champ;
  };

  const repondreAToutes = (): void => {
    saisirContenu('15000');
    repondre(0, 'Oui');
    repondre(1, 'Non');
    repondre(2, 'Vétusté déduite');
    repondre(3, 'Oui');
    repondre(4, 'Non');
    repondre(5, "L'équilibre");
  };

  const encadre = (): string => page.querySelector('.encadre-reco')?.textContent ?? '';
  const boutonVoir = (): HTMLButtonElement => page.querySelector<HTMLButtonElement>('button[type="submit"]')!;

  const valider = (): void => {
    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    devis = TestBed.inject(DevisService);
    devis.mettreAJour({ surface: '60', typeLogement: 'appartement' });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);
    creerEcran();
  });

  afterEach(() => http.verify());

  describe('AC1 : les 7 questions', () => {
    it('affiche le titre et l’étape en cours', () => {
      expect(page.querySelector('h1')?.textContent).toContain('Mes besoins');
      expect(page.querySelector('[aria-current="step"]')?.textContent).toContain('Mes besoins');
    });

    it('pose 7 questions sans aucune réponse présélectionnée', () => {
      expect(page.querySelectorAll('.libelle')).toHaveLength(7);
      expect(page.querySelector<HTMLInputElement>('#valeur-contenu')?.value).toBe('');
      expect(page.querySelector('[aria-pressed="true"]')).toBeNull();
    });

    it('enregistre chaque réponse', () => {
      repondreAToutes();
      expect(devis.besoins()).toEqual({
        valeurContenu: 15000,
        objetsValeur: true,
        appareilsCouteux: false,
        remboursement: 'vetuste',
        assistance: true,
        litiges: false,
        priorite: 'equilibre',
      });
    });

    it('met en évidence la réponse choisie', () => {
      repondre(0, 'Oui');
      expect(page.querySelectorAll('[aria-pressed="true"]')).toHaveLength(1);
    });

    it('ne garde que des chiffres dans le montant et accepte de le vider', () => {
      expect(saisirContenu('12 5a0').value).toBe('1250');
      expect(devis.besoins().valeurContenu).toBe(1250);
      saisirContenu('');
      expect(devis.besoins().valeurContenu).toBeNull();
    });
  });

  describe('AC2 : encadré « Formule conseillée »', () => {
    it('invite à répondre tant que les réponses ne suffisent pas', () => {
      expect(encadre()).toContain('Répondez aux questions pour découvrir votre formule');
      expect(page.querySelector('.encadre-tarif__prix')).toBeNull();
    });

    it('affiche la formule, son prix avant options et ses raisons', () => {
      saisirContenu('25000');
      expect(encadre()).toContain('Sérénité');
      expect(encadre()).toContain('15,04');
      expect(encadre()).toContain('Prix avant options');
      expect(encadre()).toContain('Votre contenu dépasse 20 000 €');
    });

    it('se met à jour à chaque réponse modifiée', () => {
      repondreAToutes();
      expect(encadre()).toContain('Confort');
      expect(encadre()).toContain('12,22');
      expect(encadre()).toContain('Vous déclarez des objets de valeur');

      repondre(1, 'Oui');
      expect(encadre()).toContain('Sérénité');
      expect(encadre()).toContain('Vous possédez des appareils coûteux');
    });

    it('signale un contenu au-delà du plafond de 35 000 €', () => {
      saisirContenu('35000');
      expect(page.querySelector('.encadre-reco__plafond')).toBeNull();
      saisirContenu('35001');
      expect(page.querySelector('.encadre-reco__plafond')?.textContent).toContain('35 000 €');
    });
  });

  describe('AC3 : bouton « Voir ma recommandation »', () => {
    it('reste inactif tant que les 7 questions n’ont pas de réponse', () => {
      expect(boutonVoir().disabled).toBe(true);
      saisirContenu('25000');
      expect(boutonVoir().disabled).toBe(true);
      repondreAToutes();
      expect(boutonVoir().disabled).toBe(false);
    });

    it('ne fait rien si le formulaire est incomplet', () => {
      valider();
      http.expectNone('/api/devis/recommandations');
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('enregistre la recommandation (RG4) et ouvre le choix de formule', () => {
      repondreAToutes();
      valider();
      expect(boutonVoir().disabled).toBe(true);

      const requete = http.expectOne('/api/devis/recommandations');
      expect(requete.request.body).toEqual({
        ...devis.besoins(),
        formule: 'confort',
        raisons: ['Votre contenu est compris entre 10 000 € et 20 000 €', 'Vous déclarez des objets de valeur', "Vous recherchez l'équilibre entre protection et budget"],
        options: ['objets-valeur', 'assistance-depannage'],
      });
      requete.flush({ reference: 'ref-1' }, { status: 201, statusText: 'Created' });

      expect(devis.reference()).toBe('ref-1');
      expect(devis.formuleChoisie()).toBe('confort');
      expect(router.navigate).toHaveBeenCalledWith(['/devis/formule']);
    });

    it('US-14 : repasser sans rien changer garde le devis et la formule choisie', () => {
      repondreAToutes();
      devis.validerRecommandation('ref-1', 'serenite');
      valider();

      http.expectNone('/api/devis/recommandations');
      expect(devis.reference()).toBe('ref-1');
      expect(devis.formuleChoisie()).toBe('serenite');
      expect(router.navigate).toHaveBeenCalledWith(['/devis/formule']);
    });

    it('n’envoie pas deux fois pendant l’enregistrement', () => {
      repondreAToutes();
      valider();
      valider();
      http.expectOne('/api/devis/recommandations').flush({ reference: 'ref-1' });
    });

    it('reste sur l’écran avec un message en cas d’échec, puis l’efface à la réponse suivante', () => {
      repondreAToutes();
      valider();
      http.expectOne('/api/devis/recommandations').flush(null, { status: 500, statusText: 'Erreur' });
      fixture.detectChanges();

      expect(page.querySelector('[role="alert"]')?.textContent).toContain('pas pu être enregistrée');
      expect(router.navigate).not.toHaveBeenCalled();
      expect(boutonVoir().disabled).toBe(false);

      repondre(4, 'Oui');
      expect(page.querySelector('[role="alert"]')).toBeNull();
    });
  });

  it('AC4 : explique le devoir de conseil', () => {
    expect(page.textContent).toContain('Pourquoi ces questions ?');
    expect(page.textContent).toContain('fiche conseil');
    expect(page.textContent).toContain('L521-4');
  });

  it('conserve les réponses au retour sur l’écran', () => {
    repondreAToutes();
    creerEcran();

    expect(page.querySelector<HTMLInputElement>('#valeur-contenu')?.value).toBe('15000');
    expect(page.querySelectorAll('[aria-pressed="true"]')).toHaveLength(6);
    expect(encadre()).toContain('Confort');
  });

  it('propose le retour à l’étape 1', () => {
    const lien = Array.from(page.querySelectorAll('a')).find((a) => a.textContent?.includes('Retour'));
    expect(lien?.getAttribute('href')).toBe('/devis/logement');
  });
});
