import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DevisService } from '../../core/service/devis.service';
import { DevisFormule } from './devis-formule';

describe('DevisFormule', () => {
  let fixture: ComponentFixture<DevisFormule>;
  let page: HTMLElement;
  let devis: DevisService;

  const creerEcran = (): void => {
    fixture = TestBed.createComponent(DevisFormule);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };

  const carteChoisie = (): string => page.querySelector('.formule--choisie .formule__nom')?.textContent ?? '';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    devis = TestBed.inject(DevisService);
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
    devis.validerRecommandation('ref-1', 'confort');
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
    expect(page.textContent).toContain('Objets de valeur');
    expect(page.textContent).toContain('Assistance dépannage');
    expect(page.textContent).not.toContain('Protection juridique');
  });

  it('indique quand aucune option n’est conseillée', () => {
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
});
