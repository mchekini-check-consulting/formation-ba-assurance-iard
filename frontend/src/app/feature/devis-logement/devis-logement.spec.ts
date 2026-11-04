import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { codePostalValide, colocatairesValides, estimerTarif, surfaceValide } from '../../core/model/devis';
import { DevisLogement } from './devis-logement';

describe('DevisLogement', () => {
  let fixture: ComponentFixture<DevisLogement>;
  let page: HTMLElement;
  let router: Router;

  const bouton = (texte: string): HTMLButtonElement => {
    const trouve = Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find(
      (b) => b.textContent?.trim() === texte,
    );
    if (!trouve) throw new Error(`Bouton introuvable : ${texte}`);
    return trouve;
  };

  const cliquer = (texte: string): void => {
    bouton(texte).click();
    fixture.detectChanges();
  };

  const saisir = (selecteur: string, valeur: string): HTMLInputElement => {
    const champ = page.querySelector<HTMLInputElement>(selecteur)!;
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    return champ;
  };

  const selectionner = (selecteur: string, valeur: string): HTMLSelectElement => {
    const champ = page.querySelector<HTMLSelectElement>(selecteur)!;
    champ.value = valeur;
    champ.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    return champ;
  };

  const remplirFormulaire = (): void => {
    cliquer('Locataire');
    cliquer('Bail individuel');
    selectionner('#residence', 'principale');
    selectionner('#type-logement', 'appartement');
    selectionner('#nombre-pieces', '3');
    saisir('#surface', '50');
    saisir('#adresse', '12 rue de la Paix');
    saisir('#code-postal', '75001');
    saisir('#ville', 'Paris');
    saisir('#etage', '2');
    selectionner('#sinistres', '0');
    selectionner('#resilie-contrat', 'non');
    selectionner('#antecedent-assurance', 'non-assure');
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([])],
    }).compileComponents();
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(DevisLogement);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  it('affiche le formulaire de etape 1', () => {
    expect(page.querySelector('h1')?.textContent).toContain('Mon logement et ma situation');
    expect(page.querySelector('[aria-current="step"]')?.textContent).toContain('Mon logement');
    expect(page.querySelector('[role="alert"]')).toBeNull();
  });

  it('affiche le rappel sur les fausses declarations (RG5)', () => {
    expect(page.textContent).toContain('L113-8');
    expect(page.textContent).toContain('L113-9');
  });

  describe('AC1 — champs et navigation', () => {
    it('navigue vers /devis/besoins quand le formulaire est complet et valide', () => {
      remplirFormulaire();
      bouton('Continuer vers mes besoins').click();
      fixture.detectChanges();
      expect(router.navigate).toHaveBeenCalledWith(['/devis/besoins']);
    });

    it('ne navigue pas si des champs obligatoires sont vides', () => {
      bouton('Continuer vers mes besoins').click();
      fixture.detectChanges();
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });

  describe('AC2 — questions conditionnelles', () => {
    it("affiche l'etage uniquement si appartement selectionne", () => {
      expect(page.querySelector('#etage')).toBeNull();
      selectionner('#type-logement', 'appartement');
      expect(page.querySelector('#etage')).not.toBeNull();
    });

    it("masque l'etage si on bascule vers maison", () => {
      selectionner('#type-logement', 'appartement');
      selectionner('#type-logement', 'maison');
      expect(page.querySelector('#etage')).toBeNull();
    });

    it('affiche les dependances uniquement si maison selectionnee', () => {
      expect(page.textContent).not.toContain('pendances');
      selectionner('#type-logement', 'maison');
      expect(page.textContent).toContain('pendances');
    });

    it('affiche le nombre de colocataires uniquement si colocation', () => {
      expect(page.querySelector('#colocataires')).toBeNull();
      cliquer('Locataire');
      cliquer('Colocation');
      expect(page.querySelector('#colocataires')).not.toBeNull();
    });

    it("affiche l'assureur actuel si assure depuis plus d'un an", () => {
      expect(page.querySelector('#assureur-actuel')).toBeNull();
      selectionner('#resilie-contrat', 'non');
      selectionner('#antecedent-assurance', 'assure-plus-un-an');
      expect(page.querySelector('#assureur-actuel')).not.toBeNull();
    });

    it("masque l'assureur actuel si on change l'antecedent", () => {
      selectionner('#resilie-contrat', 'non');
      selectionner('#antecedent-assurance', 'assure-plus-un-an');
      selectionner('#antecedent-assurance', 'non-assure');
      expect(page.querySelector('#assureur-actuel')).toBeNull();
    });

    it("affiche l'info loi Hamon quand assure depuis plus d'un an (RG3)", () => {
      expect(page.querySelector('.info-hamon')).toBeNull();
      selectionner('#resilie-contrat', 'non');
      selectionner('#antecedent-assurance', 'assure-plus-un-an');
      expect(page.querySelector('.info-hamon')).not.toBeNull();
      expect(page.querySelector('.info-hamon')?.textContent).toContain('Hamon');
    });

    it('affiche le champ sinistres dans le formulaire', () => {
      expect(page.querySelector('#sinistres')).not.toBeNull();
    });

    it('permet de choisir les dependances pour une maison', () => {
      selectionner('#type-logement', 'maison');
      cliquer('Oui');
      expect(
        page.querySelector<HTMLButtonElement>('[aria-labelledby="libelle-dependances"] [aria-pressed="true"]')?.textContent?.trim(),
      ).toBe('Oui');
    });

    it("enregistre le nom de l'assureur actuel", () => {
      selectionner('#resilie-contrat', 'non');
      selectionner('#antecedent-assurance', 'assure-plus-un-an');
      const champ = saisir('#assureur-actuel', 'Maif');
      expect(champ.value).toBe('Maif');
    });
  });

  describe('AC3 — proprietaire non-occupant', () => {
    it('affiche alerte non-occupant quand ce statut est choisi', () => {
      cliquer('Propriétaire, je le loue ou il est vide');
      const alerte = page.querySelector('.alerte--info');
      expect(alerte).not.toBeNull();
      expect(alerte?.textContent).toContain('non-occupant');
    });

    it('masque le reste du formulaire pour un non-occupant', () => {
      cliquer('Propriétaire, je le loue ou il est vide');
      expect(page.querySelector('#adresse')).toBeNull();
      expect(page.querySelector('#surface')).toBeNull();
      expect(page.querySelector('button[type="submit"]')).toBeNull();
    });

    it('propose un bouton Etre rappele pour un non-occupant', () => {
      cliquer('Propriétaire, je le loue ou il est vide');
      expect(bouton('Être rappelé par un conseiller')).not.toBeNull();
    });
  });

  describe('AC4 — resiliation non-paiement', () => {
    beforeEach(() => {
      cliquer('Locataire');
      cliquer('Bail individuel');
      selectionner('#residence', 'principale');
      selectionner('#type-logement', 'appartement');
      selectionner('#nombre-pieces', '2');
      saisir('#surface', '40');
      saisir('#adresse', '12 rue Rivoli');
      saisir('#code-postal', '75001');
      saisir('#ville', 'Paris');
      saisir('#etage', '1');
    });

    it('affiche alerte bloquante si resiliation non-paiement', () => {
      selectionner('#resilie-contrat', 'non-paiement');
      expect(page.querySelector('.alerte--bloquant')).not.toBeNull();
    });

    it('remplace le bouton Continuer par Etre rappele', () => {
      selectionner('#resilie-contrat', 'non-paiement');
      expect(page.querySelector('button[type="submit"]')).toBeNull();
      expect(bouton('Être rappelé par un conseiller')).not.toBeNull();
    });

    it("ne navigue pas vers l'etape 2 en cas de blocage", () => {
      selectionner('#resilie-contrat', 'non-paiement');
      bouton('Être rappelé par un conseiller').click();
      fixture.detectChanges();
      expect(router.navigate).not.toHaveBeenCalledWith(['/devis/besoins']);
    });
  });

  describe('AC5 — validation', () => {
    it('affiche des erreurs sur le premier envoi sans saisie', () => {
      bouton('Continuer vers mes besoins').click();
      fixture.detectChanges();
      const erreurs = Array.from(page.querySelectorAll('[role="alert"]'));
      expect(erreurs.length).toBeGreaterThan(0);
    });

    it('signale un code postal invalide', () => {
      cliquer('Locataire');
      selectionner('#residence', 'principale');
      selectionner('#type-logement', 'appartement');
      const cp = saisir('#code-postal', '123');
      bouton('Continuer vers mes besoins').click();
      fixture.detectChanges();
      expect(page.querySelector('#erreur-code-postal')?.textContent).toContain('5 chiffres');
      expect(cp.getAttribute('aria-invalid')).toBe('true');
    });

    it('signale une surface invalide', () => {
      cliquer('Locataire');
      selectionner('#residence', 'principale');
      selectionner('#type-logement', 'appartement');
      const s = saisir('#surface', '5');
      bouton('Continuer vers mes besoins').click();
      fixture.detectChanges();
      expect(page.querySelector('#erreur-surface')?.textContent).toContain('9');
      expect(s.getAttribute('aria-invalid')).toBe('true');
    });

    it('signale un nombre de colocataires hors plage 2-10 (RG1)', () => {
      cliquer('Locataire');
      cliquer('Colocation');
      const champ = saisir('#colocataires', '1');
      bouton('Continuer vers mes besoins').click();
      fixture.detectChanges();
      expect(page.querySelector('#erreur-colocataires')?.textContent).toContain('2');
      expect(champ.getAttribute('aria-invalid')).toBe('true');
    });

    it('replace le curseur sur assureur actuel si non renseigne (traverse focusPremierChampErreur)', () => {
      cliquer('Locataire');
      cliquer('Bail individuel');
      selectionner('#residence', 'principale');
      selectionner('#type-logement', 'appartement');
      selectionner('#nombre-pieces', '3');
      saisir('#surface', '50');
      saisir('#adresse', '12 rue de la Paix');
      saisir('#code-postal', '75001');
      saisir('#ville', 'Paris');
      saisir('#etage', '2');
      selectionner('#sinistres', '0');
      selectionner('#resilie-contrat', 'non');
      selectionner('#antecedent-assurance', 'assure-plus-un-an');
      bouton('Continuer vers mes besoins').click();
      fixture.detectChanges();
      expect(page.querySelector('#erreur-assureur')).not.toBeNull();
      expect(document.activeElement).toBe(page.querySelector<HTMLInputElement>('#assureur-actuel'));
    });

    it('signale absence de sinistres si non renseignes', () => {
      cliquer('Locataire');
      selectionner('#residence', 'principale');
      selectionner('#type-logement', 'appartement');
      selectionner('#nombre-pieces', '3');
      saisir('#surface', '50');
      saisir('#adresse', '12 rue de la Paix');
      saisir('#code-postal', '75001');
      saisir('#ville', 'Paris');
      saisir('#etage', '2');
      selectionner('#resilie-contrat', 'non');
      selectionner('#antecedent-assurance', 'non-assure');
      bouton('Continuer vers mes besoins').click();
      fixture.detectChanges();
      expect(page.querySelector('#erreur-sinistres')).not.toBeNull();
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });

  describe('AC6 — estimation du tarif', () => {
    it("affiche un message guide sans surface valide", () => {
      selectionner('#type-logement', 'appartement');
      expect(page.querySelector('.encadre-tarif')).not.toBeNull();
      expect(page.querySelector('.encadre-tarif__prix')).toBeNull();
      expect(page.querySelector('.encadre-tarif__guide')).not.toBeNull();
    });

    it('affiche encadre tarif avec surface et type valides', () => {
      selectionner('#type-logement', 'appartement');
      saisir('#surface', '60');
      expect(page.querySelector('.encadre-tarif')).not.toBeNull();
      expect(page.querySelector('.encadre-tarif__prix')?.textContent).toContain('€');
    });

    it('met a jour le tarif quand la surface change', () => {
      selectionner('#type-logement', 'appartement');
      saisir('#surface', '40');
      const tarif1 = page.querySelector('.encadre-tarif__prix')?.textContent;
      saisir('#surface', '80');
      const tarif2 = page.querySelector('.encadre-tarif__prix')?.textContent;
      expect(tarif1).not.toBe(tarif2);
    });

    it('affiche un tarif plus eleve pour une maison que pour un appartement', () => {
      selectionner('#type-logement', 'appartement');
      saisir('#surface', '80');
      const prixAppartement = page.querySelector('.encadre-tarif__prix')?.textContent ?? '';
      selectionner('#type-logement', 'maison');
      const prixMaison = page.querySelector('.encadre-tarif__prix')?.textContent ?? '';
      expect(prixMaison).not.toBe(prixAppartement);
    });
  });
});

describe('estimerTarif', () => {
  it('retourne null sans type de logement', () => {
    expect(estimerTarif('60', null)).toBeNull();
  });

  it('retourne null avec surface invalide', () => {
    expect(estimerTarif('5', 'appartement')).toBeNull();
    expect(estimerTarif('', 'appartement')).toBeNull();
  });

  it('calcule un tarif pour un appartement', () => {
    const tarif = estimerTarif('60', 'appartement');
    expect(tarif).not.toBeNull();
    expect(tarif!).toBeGreaterThan(7.9);
  });

  it('calcule un tarif maison superieur au tarif appartement', () => {
    expect(estimerTarif('60', 'maison')!).toBeGreaterThan(estimerTarif('60', 'appartement')!);
  });
});

describe('surfaceValide', () => {
  it.each([
    ['9', true],
    ['1000', true],
    ['65', true],
    ['8', false],
    ['1001', false],
    ['', false],
    ['abc', false],
    ['65.5', false],
  ])('%s valide -> %s', (s, attendu) => {
    expect(surfaceValide(s)).toBe(attendu);
  });
});

describe('colocatairesValides (RG1)', () => {
  it.each([
    ['2', true],
    ['10', true],
    ['5', true],
    ['1', false],
    ['11', false],
    ['', false],
    ['abc', false],
  ])('%s valide -> %s', (s, attendu) => {
    expect(colocatairesValides(s)).toBe(attendu);
  });
});

describe('codePostalValide (devis)', () => {
  it.each([
    ['75001', true],
    ['1234', false],
    ['123456', false],
    ['', false],
  ])('%s -> %s', (cp, attendu) => {
    expect(codePostalValide(cp)).toBe(attendu);
  });
});
