import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AnnonceService } from '../../core/service/annonce.service';
import { Landing, TITRE_PRINCIPAL, codePostalValide } from './landing';

describe('Landing', () => {
  let fixture: ComponentFixture<Landing>;
  let page: HTMLElement;
  let router: Router;

  const bouton = (texte: string): HTMLButtonElement => {
    const trouve = Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.trim() === texte);
    if (!trouve) {
      throw new Error(`Bouton « ${texte} » introuvable`);
    }
    return trouve;
  };

  const cliquer = (texte: string): void => {
    bouton(texte).click();
    fixture.detectChanges();
  };

  const saisirCodePostal = (valeur: string): HTMLInputElement => {
    const champ = page.querySelector<HTMLInputElement>('#code-postal')!;
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    return champ;
  };

  const annonce = (): string | undefined => page.querySelector('[role="status"]')?.textContent?.trim();

  beforeEach(() => {
    sessionStorage.removeItem('devis-auto-mode');
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(Landing);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  it('affiche toutes les sections de la page', () => {
    const titres = Array.from(page.querySelectorAll('h1, h2')).map((t) => t.textContent?.trim());
    expect(titres).toEqual([
      TITRE_PRINCIPAL,
      'Votre tarif habitation',
      'Votre tarif auto',
      "L'assurance habitation Assur'Easy",
      "L'assurance auto Assur'Easy",
      'Un sinistre ? Déclarez-le en ligne',
      'Aide',
    ]);
    expect(page.querySelectorAll('.etape')).toHaveLength(3);
    expect(page.querySelectorAll('.faq')).toHaveLength(3);
  });

  it('n\'affiche plus la section « Assuré en 4 étapes » ni le bandeau final', () => {
    const titresH2 = Array.from(page.querySelectorAll('h2')).map((h) => h.textContent?.trim());
    expect(titresH2).not.toContain('Assuré en 4 étapes');
    expect(titresH2).not.toContain('Prêt à assurer votre logement ?');
  });

  it("affiche l'image principale en fond du haut de page, autour de l'accroche et du formulaire", () => {
    const hautDePage = page.querySelector('main section.bloc--image')!;
    expect(hautDePage).toBeTruthy();
    expect(hautDePage.querySelector('h1')).toBeTruthy();
    expect(hautDePage.querySelector('.tarif')).toBeTruthy();
  });

  it("présente l'offre habitation, à partir de 7,90 €/mois TTC", () => {
    const section = page.querySelector('#section-offre-habitation')!;
    expect(section.querySelector('.prix')?.textContent).toContain('7,90 €/mois TTC');
    expect(Array.from(section.querySelectorAll('.carte h3')).map((h) => h.textContent?.trim())).toEqual([
      'Essentielle',
      'Confort',
      'Sérénité',
    ]);
  });

  it('affiche le picto de chaque formule à côté de son nom', () => {
    const section = page.querySelector('#section-offre-habitation')!;
    expect(Array.from(section.querySelectorAll('.carte h3 img.carte__picto')).map((i) => i.getAttribute('src'))).toEqual([
      'picto-essentielle.png',
      'picto-confort.png',
      'picto-serenite.png',
    ]);
  });

  it('« Faire mon devis habitation » navigue vers l\'étape 1 du devis habitation', () => {
    cliquer('Faire mon devis habitation');
    expect(router.navigate).toHaveBeenCalledWith(['/devis/logement']);
  });

  it("affiche l'en-tête avec le logo et les 3 boutons d'accès", () => {
    const entete = page.querySelector('header')!;
    expect(entete.textContent).toContain("Assur'Easy");
    expect(entete.querySelector('.logo img')?.getAttribute('src')).toBe('favicon.png');
    expect(Array.from(entete.querySelectorAll('.entete__actions > *')).map((b) => b.textContent?.trim())).toEqual([
      'Espace client',
      'Créer un compte',
      'Devis gratuit',
    ]);
  });

  it('« Créer un compte » mène à la création de compte (US-05)', () => {
    const lien = Array.from(page.querySelectorAll('a')).find((a) => a.textContent?.trim() === 'Créer un compte');
    expect(lien?.getAttribute('href')).toBe('/inscription');
  });

  it('« Espace client » mène à l\'espace client (US-06)', () => {
    const lien = Array.from(page.querySelectorAll('a')).find((a) => a.textContent?.trim() === 'Espace client');
    expect(lien?.getAttribute('href')).toBe('/espace-client');
  });

  it("affiche les mentions d'assureur et les liens légaux en pied de page", () => {
    const pied = page.querySelector('footer')!;
    expect(pied.textContent).toContain('ACPR');
    expect(pied.textContent).toContain('Code des assurances');
    expect(Array.from(pied.querySelectorAll('.lien')).map((l) => l.textContent?.trim())).toEqual([
      'Mentions légales',
      'Données personnelles',
      'Cookies',
      'CGU',
      'Accessibilité',
      'Réclamations',
      'Contact',
    ]);
    expect(Array.from(pied.querySelectorAll('a.lien')).map((l) => l.getAttribute('href'))).toEqual([
      '/cookies',
      '/cgu',
      '/accessibilite',
      '/contact',
    ]);
  });

  it("affiche une annonce quand on clique sur un lien non encore disponible", () => {
    expect(annonce()).toBeUndefined();
    cliquer('Mentions légales');
    expect(annonce()).toContain('bientôt disponible');
  });

  it('affiche le message préparé avant le retour à l’accueil (US-07, AC3)', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    TestBed.inject(AnnonceService).preparer('Vous êtes bien déconnecté.');
    const accueil = TestBed.createComponent(Landing);
    accueil.detectChanges();

    expect((accueil.nativeElement as HTMLElement).querySelector('[role="status"]')?.textContent).toContain(
      'Vous êtes bien déconnecté.',
    );
  });

  it("ferme le message bientot disponible", () => {
    cliquer('Mentions légales');
    page.querySelector<HTMLButtonElement>('[aria-label="Fermer le message"]')!.click();
    fixture.detectChanges();
    expect(annonce()).toBeUndefined();
  });

  it("Devis gratuit navigue vers etape 1 du devis (US-09)", () => {
    cliquer('Devis gratuit');
    expect(router.navigate).toHaveBeenCalledWith(['/devis/logement']);
    expect(annonce()).toBeUndefined();
  });

  it('déplie et replie la réponse sous la question cliquée', () => {
    const question = page.querySelectorAll<HTMLButtonElement>('.faq__question')[1];
    expect(page.querySelector('.faq__reponse')).toBeNull();

    question.click();
    fixture.detectChanges();
    expect(question.getAttribute('aria-expanded')).toBe('true');
    expect(page.querySelector('#faq-reponse-1')?.textContent).toContain('loi Hamon');

    question.click();
    fixture.detectChanges();
    expect(question.getAttribute('aria-expanded')).toBe('false');
    expect(page.querySelector('.faq__reponse')).toBeNull();
  });

  it("n'ouvre qu'une question à la fois", () => {
    const faqQuestions = page.querySelectorAll<HTMLButtonElement>('.faq__question');
    faqQuestions[0].click();
    faqQuestions[2].click();
    fixture.detectChanges();
    expect(page.querySelectorAll('.faq__reponse')).toHaveLength(1);
    expect(page.querySelector('#faq-reponse-2')).not.toBeNull();
  });

  describe('section « L\'assurance auto Assur\'Easy »', () => {
    it('affiche le badge Nouveau, le titre, le prix et les 3 formules', () => {
      const section = page.querySelector('#section-offre-auto')!;
      expect(section.querySelector('.badge')?.textContent?.trim()).toBe('Nouveau');
      expect(section.querySelector('h2')?.textContent?.trim()).toBe("L'assurance auto Assur'Easy");
      expect(section.querySelector('.prix')?.textContent).toContain('25,00 €/mois TTC');
      expect(Array.from(section.querySelectorAll('.carte h3')).map((h) => h.textContent?.trim())).toEqual([
        'Tiers',
        'Intermédiaire',
        'Tous risques',
      ]);
    });

    it('« Faire mon devis auto » navigue vers l\'étape 1.1 sans mode', () => {
      cliquer('Faire mon devis auto');
      expect(router.navigate).toHaveBeenCalledWith(['/devis-auto/vehicule']);
    });
  });

  describe('section sinistre', () => {
    it('affiche le titre, le sous-titre et les 3 étapes', () => {
      const section = page.querySelector('[aria-labelledby="sinistre-titre"]')!;
      expect(section.querySelector('h2')?.textContent?.trim()).toBe('Un sinistre ? Déclarez-le en ligne');
      expect(section.querySelectorAll('.etape')).toHaveLength(3);
    });

    it('affiche l\'encadré urgence avec le texte et le numéro d\'assistance, après le bouton', () => {
      const section = page.querySelector('[aria-labelledby="sinistre-titre"]')!;
      const urgence = section.querySelector('.urgence')!;
      expect(urgence.textContent).toContain('urgence');
      expect(urgence.textContent).toContain('112');
      const boutonDeclarer = Array.from(section.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Déclarer un sinistre')!;
      expect(boutonDeclarer.compareDocumentPosition(urgence) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('« Déclarer un sinistre » navigue vers la connexion', () => {
      cliquer('Déclarer un sinistre');
      expect(router.navigate).toHaveBeenCalledWith(['/connexion']);
    });
  });

  describe('onglets Habitation / Auto (US-26)', () => {
    it('affiche l\'onglet Habitation actif par defaut (RG1)', () => {
      const ongletHabitation = page.querySelector<HTMLButtonElement>('#onglet-habitation')!;
      const ongletAuto = page.querySelector<HTMLButtonElement>('#onglet-auto')!;
      expect(ongletHabitation.getAttribute('aria-selected')).toBe('true');
      expect(ongletAuto.getAttribute('aria-selected')).toBe('false');
      expect(page.querySelector('#panneau-habitation')?.hasAttribute('hidden')).toBe(false);
      expect(page.querySelector('#panneau-auto')?.hasAttribute('hidden')).toBe(true);
    });

    it('bascule vers l\'onglet Auto au clic', () => {
      cliquer('Auto');
      expect(page.querySelector('#onglet-auto')?.getAttribute('aria-selected')).toBe('true');
      expect(page.querySelector('#onglet-habitation')?.getAttribute('aria-selected')).toBe('false');
      expect(page.querySelector('#panneau-auto')?.hasAttribute('hidden')).toBe(false);
      expect(page.querySelector('#panneau-habitation')?.hasAttribute('hidden')).toBe(true);
    });

    it('revient a Habitation au second clic', () => {
      cliquer('Auto');
      cliquer('Habitation');
      expect(page.querySelector('#onglet-habitation')?.getAttribute('aria-selected')).toBe('true');
      expect(page.querySelector('#panneau-habitation')?.hasAttribute('hidden')).toBe(false);
    });

    it('gere la navigation clavier ArrowRight et ArrowLeft dans le tablist', () => {
      const tablist = page.querySelector('[role="tablist"]')!;

      tablist.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
      fixture.detectChanges();
      expect(page.querySelector('#onglet-auto')?.getAttribute('aria-selected')).toBe('true');

      tablist.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
      fixture.detectChanges();
      expect(page.querySelector('#onglet-habitation')?.getAttribute('aria-selected')).toBe('true');
    });

    it('respecte le pattern WAI-ARIA Tabs', () => {
      expect(page.querySelector('[role="tablist"]')).toBeTruthy();
      expect(page.querySelectorAll('[role="tab"]')).toHaveLength(2);
      expect(page.querySelectorAll('[role="tabpanel"]')).toHaveLength(2);
      const ongletHabitation = page.querySelector('#onglet-habitation')!;
      expect(ongletHabitation.getAttribute('aria-controls')).toBe('panneau-habitation');
      expect(page.querySelector('#panneau-habitation')?.getAttribute('aria-labelledby')).toBe('onglet-habitation');
    });

    describe('formulaire « Votre tarif auto »', () => {
      it('aucun choix prés-électionné à l\'ouverture de l\'onglet Auto', () => {
        cliquer('Auto');
        expect(page.querySelector('[role="radio"][aria-checked="true"]')).toBeNull();
      });

      it('clic sans choix → message erreur, pas de navigation', () => {
        cliquer('Auto');
        cliquer('Commencer mon devis auto');
        expect(page.querySelector('#erreur-mode-auto')?.textContent).toContain('Choisissez comment retrouver votre véhicule');
        expect(router.navigate).not.toHaveBeenCalled();
      });

      it('le message disparaît dès qu\'un choix est fait', () => {
        cliquer('Auto');
        cliquer('Commencer mon devis auto');
        expect(page.querySelector('#erreur-mode-auto')).not.toBeNull();
        cliquer('Je connais ma plaque');
        expect(page.querySelector('#erreur-mode-auto')).toBeNull();
      });

      it('choix « Je connais ma plaque » → navigue vers étape 1.1 avec mode=plaque', () => {
        cliquer('Auto');
        cliquer('Je connais ma plaque');
        cliquer('Commencer mon devis auto');
        expect(router.navigate).toHaveBeenCalledWith(
          ['/devis-auto/vehicule'],
          { state: { mode: 'plaque' }, queryParams: { mode: 'plaque' } },
        );
      });

      it('choix « Je choisis par marque » → navigue vers étape 1.1 avec mode=marque', () => {
        cliquer('Auto');
        cliquer('Je choisis par marque');
        cliquer('Commencer mon devis auto');
        expect(router.navigate).toHaveBeenCalledWith(
          ['/devis-auto/vehicule'],
          { state: { mode: 'marque' }, queryParams: { mode: 'marque' } },
        );
      });

      it('navigation clavier ArrowRight dans le radiogroup sélectionne les options', () => {
        cliquer('Auto');
        const radiogroup = page.querySelector('[role="radiogroup"]')!;

        radiogroup.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
        fixture.detectChanges();
        expect(page.querySelector('[role="radio"][aria-checked="true"]')?.textContent?.trim()).toBe('Je connais ma plaque');

        radiogroup.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
        fixture.detectChanges();
        expect(page.querySelector('[role="radio"][aria-checked="true"]')?.textContent?.trim()).toBe('Je choisis par marque');
      });

      it('navigation clavier ArrowLeft dans le radiogroup sélectionne les options', () => {
        cliquer('Auto');
        const radiogroup = page.querySelector('[role="radiogroup"]')!;

        radiogroup.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
        fixture.detectChanges();
        expect(page.querySelector('[role="radio"][aria-checked="true"]')?.textContent?.trim()).toBe('Je choisis par marque');
      });

      it('respecte le pattern WAI-ARIA radiogroup', () => {
        cliquer('Auto');
        const radiogroup = page.querySelector('[role="radiogroup"]')!;
        expect(radiogroup.getAttribute('aria-labelledby')).toBe('libelle-mode-auto');
        expect(page.querySelectorAll('#panneau-auto [role="radio"]')).toHaveLength(2);
      });

      it('la tuile choisie reste sélectionnée si l\'utilisateur revient à l\'accueil', () => {
        sessionStorage.setItem('devis-auto-mode', 'marque');
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
        const accueil = TestBed.createComponent(Landing);
        accueil.detectChanges();
        const accueilPage = accueil.nativeElement as HTMLElement;

        const btnMarque = Array.from(accueilPage.querySelectorAll('[role="radio"]')).find(
          (b) => b.textContent?.includes('Je choisis par marque'),
        );
        expect(btnMarque?.getAttribute('aria-checked')).toBe('true');
        sessionStorage.removeItem('devis-auto-mode');
      });
    });
  });

  describe('formulaire « Votre tarif habitation »', () => {
    it('marque le choix sélectionné', () => {
      cliquer('Propriétaire');
      cliquer('Maison');
      expect(bouton('Propriétaire').getAttribute('aria-pressed')).toBe('true');
      expect(bouton('Locataire').getAttribute('aria-pressed')).toBe('false');
      expect(bouton('Maison').classList).toContain('choix__option--actif');
    });

    it('ne garde que 5 chiffres dans le code postal', () => {
      expect(saisirCodePostal('9a40-001').value).toBe('94000');
    });

    it("n'affiche aucune erreur avant le premier clic", () => {
      expect(page.querySelector('[role="alert"]')).toBeNull();
    });

    it('signale un code postal invalide et replace le curseur dans le champ', () => {
      cliquer('Locataire');
      cliquer('Appartement');
      const champ = saisirCodePostal('940');
      cliquer('Calculer mon tarif');

      expect(page.querySelector('#erreur-code-postal')?.textContent).toContain('code postal à 5 chiffres');
      expect(champ.getAttribute('aria-invalid')).toBe('true');
      expect(document.activeElement).toBe(champ);
      expect(annonce()).toBeUndefined();
    });

    it('demande le statut et le type de logement', () => {
      saisirCodePostal('94000');
      cliquer('Calculer mon tarif');
      expect(page.textContent).toContain('Indiquez votre statut et le type de logement.');
      expect(annonce()).toBeUndefined();
    });

    it("navigue vers le devis quand le formulaire est complet (US-09)", () => {
      cliquer('Locataire');
      cliquer('Appartement');
      saisirCodePostal('94000');
      cliquer('Calculer mon tarif');
      expect(page.querySelector('[role="alert"]')).toBeNull();
      expect(annonce()).toBeUndefined();
      expect(router.navigate).toHaveBeenCalledWith(['/devis/logement']);
    });
  });
});

describe('codePostalValide', () => {
  it.each([
    ['94000', true],
    ['75001', true],
    ['9400', false],
    ['940000', false],
    ['94 00', false],
    ['', false],
  ])('%s → %s', (codePostal, attendu) => {
    expect(codePostalValide(codePostal)).toBe(attendu);
  });
});
