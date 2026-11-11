import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AccesNonAutorise } from './acces-non-autorise';

describe('AccesNonAutorise (US-04, AC2)', () => {
  it('affiche « Accès non autorisé » et un retour vers mon espace, sans autre donnée', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(AccesNonAutorise);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;

    expect(page.querySelector('h1')?.textContent).toBe('Accès non autorisé');
    expect(page.querySelector('[role="alert"]')).not.toBeNull();
    expect(page.querySelector('main a')?.getAttribute('href')).toBe('/espace-client');
  });
});
