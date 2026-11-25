import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DevisAutoFormule } from './devis-auto-formule';
import { DevisAutoService } from '../../core/service/devis-auto.service';

describe('DevisAutoFormule', () => {
  let fixture: ComponentFixture<DevisAutoFormule>;
  let component: DevisAutoFormule;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DevisAutoFormule],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DevisAutoFormule);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('formaterPrix formate avec 2 décimales', () => {
    expect(component['formaterPrix'](45)).toBe('45,00');
  });

  it('libelle retourne "Tous risques" pour TOUS_RISQUES', () => {
    expect(component['libelle']('TOUS_RISQUES')).toBe('Tous risques');
  });

  it('libelle retourne "Intermédiaire" pour INTERMEDIAIRE', () => {
    expect(component['libelle']('INTERMEDIAIRE')).toBe('Intermédiaire');
  });

  it('libelle retourne "Tiers" par défaut', () => {
    expect(component['libelle']('TIERS')).toBe('Tiers');
  });

  it("affiche le lien retour aux besoins quand pas de recommandation", () => {
    const service = TestBed.inject(DevisAutoService);
    service.recommandation.set(null);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('a[href]');
    expect(link).toBeTruthy();
  });

  it("affiche la formule quand une recommandation est disponible", () => {
    const service = TestBed.inject(DevisAutoService);
    service.recommandation.set({ formuleConseillee: 'TOUS_RISQUES', raisons: ['Véhicule récent'], prixMensuel: 45 });
    fixture.detectChanges();
    const contenu = fixture.nativeElement.textContent;
    expect(contenu).toContain('Tous risques');
  });
});
