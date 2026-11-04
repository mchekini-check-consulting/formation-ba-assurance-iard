import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DevisBesoins } from './devis-besoins';

describe('DevisBesoins', () => {
  let fixture: ComponentFixture<DevisBesoins>;
  let page: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    fixture = TestBed.createComponent(DevisBesoins);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  it('affiche le titre de l\'étape besoins', () => {
    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Vos besoins');
  });

  it('propose un lien de retour vers l\'étape logement', () => {
    const lien = Array.from(page.querySelectorAll('a')).find((a) => a.textContent?.includes('Retour'));
    expect(lien?.getAttribute('href')).toBe('/devis/logement');
  });
});
