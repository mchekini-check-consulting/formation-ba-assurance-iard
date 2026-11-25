import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DevisAutoLayout } from './devis-auto-layout';

describe('DevisAutoLayout', () => {
  const creer = (sousEtape: '1.1' | '1.2' | '1.3' | '1.4' | '2') => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(DevisAutoLayout);
    fixture.componentRef.setInput('sousEtape', sousEtape);
    fixture.detectChanges();
    return fixture;
  };

  it('affiche le stepper 6 etapes avec Etape 1 active', () => {
    const fixture = creer('1.1');
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Mon véhicule');
    expect(texte).toContain('Mes besoins');
    expect(texte).toContain('Ma formule');
    expect(texte).toContain('Mes informations');
    expect(texte).toContain('Paiement');
    expect(texte).toContain('Mon contrat');
  });

  it('1.1 active : aucune sous-etape terminee', () => {
    const fixture = creer('1.1');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.sous-etape--terminee')).toBeNull();
    expect(el.querySelector('.sous-etape--active')?.textContent).toContain('1.1');
  });

  it('1.2 active : 1.1 terminee et cliquable', () => {
    const fixture = creer('1.2');
    const el = fixture.nativeElement as HTMLElement;
    const terminees = el.querySelectorAll('.sous-etape--terminee');
    expect(terminees.length).toBe(1);
    expect(terminees[0].tagName.toLowerCase()).toBe('a');
    expect(terminees[0].textContent).toContain('1.1');
    expect(el.querySelector('.sous-etape--active')?.textContent).toContain('1.2');
  });

  it('1.3 active : 1.1 et 1.2 terminees et cliquables', () => {
    const fixture = creer('1.3');
    const el = fixture.nativeElement as HTMLElement;
    const terminees = el.querySelectorAll('.sous-etape--terminee');
    expect(terminees.length).toBe(2);
    expect(Array.from(terminees).every(t => t.tagName.toLowerCase() === 'a')).toBe(true);
    expect(el.querySelector('.sous-etape--active')?.textContent).toContain('1.3');
  });

  it('1.4 active : 1.1, 1.2 et 1.3 terminees et cliquables', () => {
    const fixture = creer('1.4');
    const el = fixture.nativeElement as HTMLElement;
    const terminees = el.querySelectorAll('.sous-etape--terminee');
    expect(terminees.length).toBe(3);
    expect(el.querySelector('.sous-etape--active')?.textContent).toContain('1.4');
  });

  it('affiche "Deja client" et "Quitter le devis" dans l\'entete', () => {
    const fixture = creer('1.1');
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Déjà client');
    expect(texte).toContain('Se connecter');
    expect(texte).toContain('Quitter le devis');
  });

  it('etape 2 active : etape 2 en bordeaux, etape 1 faite', () => {
    const fixture = creer('2');
    const el = fixture.nativeElement as HTMLElement;
    const etapes = el.querySelectorAll('.progression__etape');
    expect(etapes[0].classList.contains('progression__etape--faite')).toBe(true);
    expect(etapes[1].classList.contains('progression__etape--active')).toBe(true);
  });

  it('etape 2 : pas de sous-etapes affichees', () => {
    const fixture = creer('2');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.sous-etapes')).toBeNull();
  });
});
