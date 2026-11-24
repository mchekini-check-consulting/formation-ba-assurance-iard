import { TestBed } from '@angular/core/testing';
import { decouperPlaques } from '../../core/model/texte';
import { TextePlaque } from './texte-plaque';

describe('immatriculations insécables (US-21, recette)', () => {
  it('isole chaque plaque, sans changer le texte', () => {
    const morceaux = decouperPlaques('Peugeot 208 · EF-003-GH');
    expect(morceaux).toEqual([
      { texte: 'Peugeot 208 · ', plaque: false },
      { texte: 'EF-003-GH', plaque: true },
    ]);
    expect(morceaux.map((m) => m.texte).join('')).toBe('Peugeot 208 · EF-003-GH');
  });

  it('plusieurs plaques, un texte sans plaque, un texte vide', () => {
    expect(decouperPlaques('AB-001-CD et EF-002-GH').filter((m) => m.plaque).map((m) => m.texte)).toEqual([
      'AB-001-CD',
      'EF-002-GH',
    ]);
    expect(decouperPlaques('Appartement · Créteil')).toEqual([{ texte: 'Appartement · Créteil', plaque: false }]);
    expect(decouperPlaques(null)).toEqual([]);
  });

  it('la plaque est rendue dans un élément qui ne revient jamais à la ligne ; le texte affiché est identique', () => {
    TestBed.configureTestingModule({});
    const fixture = TestBed.createComponent(TextePlaque);
    fixture.componentRef.setInput('texte', 'Peugeot 208 · EF-003-GH');
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toBe('Peugeot 208 · EF-003-GH');
    const plaque = element.querySelector('.plaque') as HTMLElement;
    expect(plaque.textContent).toBe('EF-003-GH');
    expect(getComputedStyle(plaque).whiteSpace).toBe('nowrap');
  });
});
