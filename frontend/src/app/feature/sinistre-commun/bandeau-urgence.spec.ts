import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BandeauUrgence } from './bandeau-urgence';
import { EtapesSinistre } from './etapes-sinistre';

describe('BandeauUrgence (US-24, AC10)', () => {
  let fixture: ComponentFixture<BandeauUrgence>;
  let page: HTMLElement;

  const afficher = (produit: 'AUTO' | 'HABITATION' | null, type: string | null, gestes: string[] = []): void => {
    fixture = TestBed.createComponent(BandeauUrgence);
    fixture.componentRef.setInput('numero', '01 23 45 67 89');
    fixture.componentRef.setInput('produit', produit);
    fixture.componentRef.setInput('type', type);
    fixture.componentRef.setInput('gestes', gestes);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  };

  it('numéro d’assistance 24h/24 sur chaque étape', () => {
    afficher(null, null);
    expect(page.textContent).toContain('Assistance 24h/24');
    expect(page.querySelector('a')?.getAttribute('href')).toBe('tel:0123456789');
    expect(page.textContent).not.toContain('Demander un dépannage');
  });

  it('accident auto : bouton « Demander un dépannage »', () => {
    afficher('AUTO', 'ACCIDENT_TIERS');
    const depannage = Array.from(page.querySelectorAll('a')).find((a) => a.textContent?.includes('dépannage'));
    expect(depannage?.getAttribute('href')).toBe('tel:0123456789');
  });

  it('habitation : les gestes d’urgence du type de sinistre', () => {
    afficher('HABITATION', 'DEGAT_EAUX', ['Coupez l’arrivée d’eau au robinet principal.']);
    expect(page.textContent).toContain('Les bons gestes, tout de suite');
    expect(page.querySelector('li')?.textContent).toBe('Coupez l’arrivée d’eau au robinet principal.');
    expect(page.textContent).not.toContain('Demander un dépannage');
  });
});

describe('EtapesSinistre (US-24)', () => {
  it('signale l’étape en cours', () => {
    const fixture = TestBed.createComponent(EtapesSinistre);
    fixture.componentRef.setInput('etape', 2);
    fixture.detectChanges();
    const etapes = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('li'));
    expect(etapes.map((li) => li.getAttribute('aria-current'))).toEqual([null, 'step', null]);
    expect(etapes[0].classList).toContain('etapes__etape--faite');
  });
});
