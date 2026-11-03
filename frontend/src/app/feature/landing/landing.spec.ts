import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Landing, codePostalValide } from './landing';

describe('Landing', () => {
  let fixture: ComponentFixture<Landing>;
  let page: HTMLElement;

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
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    fixture = TestBed.createComponent(Landing);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  it('affiche toutes les sections de la page', () => {
    const titres = Array.from(page.querySelectorAll('h1, h2')).map((t) => t.textContent?.trim());
    expect(titres).toEqual([
      'Votre logement assuré en ligne, sans conseiller ni paperasse.',
      'Votre tarif habitation',
      "L'assurance habitation Assur'Easy",
      'Assuré en 4 étapes',
      'Questions fréquentes',
      'Prêt à assurer votre logement ?',
    ]);
    expect(page.querySelectorAll('.etape')).toHaveLength(4);
    expect(page.querySelectorAll('.faq')).toHaveLength(4);
  });

  it("présente uniquement l'offre habitation, à partir de 7,90 €/mois TTC", () => {
    expect(page.querySelector('.prix')?.textContent).toContain('7,90 €/mois TTC');
    expect(Array.from(page.querySelectorAll('.carte h3')).map((h) => h.textContent)).toEqual([
      'Essentielle',
      'Confort',
      'Sérénité',
    ]);
    expect(page.textContent).not.toMatch(/auto(mobile)?\b/i);
  });

  it("affiche l'en-tête avec le logo et les 3 boutons d'accès", () => {
    const entete = page.querySelector('header')!;
    expect(entete.textContent).toContain("Assur'Easy");
    expect(entete.querySelector('.logo img')?.getAttribute('src')).toBe('favicon.png');
    expect(Array.from(entete.querySelectorAll('button')).map((b) => b.textContent?.trim())).toEqual([
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

  it("affiche les mentions d'assureur et les liens légaux en pied de page", () => {
    const pied = page.querySelector('footer')!;
    expect(pied.textContent).toContain('RCS Paris 912 345 678');
    expect(pied.textContent).toContain('ACPR');
    expect(pied.textContent).toContain("La Médiation de l'Assurance");
    expect(pied.textContent).toContain("© 2026 Assur'Easy. Tous droits réservés.");
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

  it.each([
    ['Espace client', 'L’espace client sera bientôt disponible.'],
    ['Devis gratuit', 'Le devis en ligne sera bientôt disponible.'],
    ['Faire mon devis', 'Le devis en ligne sera bientôt disponible.'],
  ])('« %s » annonce que le service sera bientôt disponible', (texte, message) => {
    expect(annonce()).toBeUndefined();
    cliquer(texte);
    expect(annonce()).toContain(message);
  });

  it('ferme le message « bientôt disponible »', () => {
    cliquer('Devis gratuit');
    page.querySelector<HTMLButtonElement>('[aria-label="Fermer le message"]')!.click();
    fixture.detectChanges();
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
    const questions = page.querySelectorAll<HTMLButtonElement>('.faq__question');
    questions[0].click();
    questions[2].click();
    fixture.detectChanges();
    expect(page.querySelectorAll('.faq__reponse')).toHaveLength(1);
    expect(page.querySelector('#faq-reponse-2')).not.toBeNull();
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

    it('annonce que le calcul sera bientôt disponible quand tout est rempli', () => {
      cliquer('Locataire');
      cliquer('Appartement');
      saisirCodePostal('94000');
      cliquer('Calculer mon tarif');
      expect(page.querySelector('[role="alert"]')).toBeNull();
      expect(annonce()).toContain('Le calcul de votre tarif sera bientôt disponible.');
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
