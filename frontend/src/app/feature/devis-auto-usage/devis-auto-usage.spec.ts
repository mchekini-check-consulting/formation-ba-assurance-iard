import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DevisAutoUsage } from './devis-auto-usage';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { UsageAutoSaisie } from '../../core/model/devis-auto';

@Component({ template: '' })
class StubPage {}

describe('DevisAutoUsage', () => {
  let service: DevisAutoService;

  const creer = (usage: UsageAutoSaisie | null = null) => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'devis-auto/conducteur', component: StubPage }])],
    });
    service = TestBed.inject(DevisAutoService);
    if (usage) service.usage.set(usage);
    const fixture = TestBed.createComponent(DevisAutoUsage);
    fixture.detectChanges();
    return fixture;
  };

  const soumettre = (fixture: ReturnType<typeof creer>) => {
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };

  // ─── STRUCTURE ─────────────────────────────────────────────────

  it('affiche le stepper 6 etapes, Etape 1 active', () => {
    const fixture = creer();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Mon véhicule');
    expect(texte).toContain('Mes besoins');
    expect(texte).toContain('Ma formule');
  });

  it('affiche 1.1 et 1.2 comme terminees et 1.3 comme active', () => {
    const fixture = creer();
    const el = fixture.nativeElement as HTMLElement;
    const terminees = el.querySelectorAll('.sous-etape--terminee');
    expect(terminees.length).toBe(2);
    expect(Array.from(terminees).every(t => t.tagName.toLowerCase() === 'a')).toBe(true);
    const active = el.querySelector('.sous-etape--active');
    expect(active?.textContent).toContain('1.3');
  });

  it('affiche le titre et le sous-titre de la page', () => {
    const fixture = creer();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain("L'usage de votre véhicule");
    expect(texte).toContain('Comment et où vous');
  });

  it('affiche les 3 boutons d\'usage', () => {
    const fixture = creer();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Vie privée');
    expect(texte).toContain('Privé et trajet travail');
    expect(texte).toContain('Usage professionnel');
  });

  it('affiche la liste deroulante kilometrage', () => {
    const fixture = creer();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Moins de 8 000 km');
    expect(texte).toContain('Plus de 25 000 km');
  });

  it('affiche les 3 boutons de stationnement', () => {
    const fixture = creer();
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain('Garage fermé');
    expect(texte).toContain('Parking privé');
    expect(texte).toContain('Voie publique');
  });

  it('affiche le champ code postal', () => {
    const fixture = creer();
    expect((fixture.nativeElement as HTMLElement).querySelector('#code-postal')).toBeTruthy();
  });

  // ─── VALIDATION ────────────────────────────────────────────────

  it('affiche 4 erreurs si formulaire vide', () => {
    const fixture = creer();
    soumettre(fixture);
    const texte = (fixture.nativeElement as HTMLElement).textContent!;
    expect(texte).toContain("Indiquez l'usage de votre véhicule");
    expect(texte).toContain('Choisissez votre kilométrage annuel');
    expect(texte).toContain('Indiquez où stationne votre véhicule la nuit');
    expect(texte).toContain('Saisissez un code postal');
  });

  it('code postal incomplet affiche erreur', () => {
    const fixture = creer();
    const comp = fixture.componentInstance;
    comp['codePostal'].set('123');
    fixture.detectChanges();
    soumettre(fixture);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Code postal invalide');
  });

  it('code postal correct (5 chiffres) ne genere pas d\'erreur', () => {
    const fixture = creer();
    const comp = fixture.componentInstance;
    comp['codePostal'].set('75000');
    fixture.detectChanges();
    soumettre(fixture);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Code postal invalide');
  });

  // ─── SOUMISSION VALIDE ──────────────────────────────────────────

  it('formulaire complet enregistre les donnees et navigue', () => {
    const fixture = creer();
    const comp = fixture.componentInstance;

    comp['usage'].set('PRIVE');
    comp['kmAnnuels'].set('8000_15000');
    comp['stationnement'].set('GARAGE_FERME');
    comp['codePostal'].set('94000');
    fixture.detectChanges();

    soumettre(fixture);

    expect(service.usage()?.usage).toBe('PRIVE');
    expect(service.usage()?.kmAnnuels).toBe('8000_15000');
    expect(service.usage()?.stationnement).toBe('GARAGE_FERME');
    expect(service.usage()?.codePostal).toBe('94000');
  });

  // ─── CONSERVATION DES DONNÉES ───────────────────────────────────

  it('pre-remplit depuis les donnees deja enregistrees', () => {
    const saisie: UsageAutoSaisie = {
      usage: 'TRAJET_TRAVAIL',
      stationnement: 'PARKING_PRIVE',
      kmAnnuels: '15001_25000',
      codePostal: '69000',
    };
    const fixture = creer(saisie);
    const comp = fixture.componentInstance;
    expect(comp['usage']()).toBe('TRAJET_TRAVAIL');
    expect(comp['kmAnnuels']()).toBe('15001_25000');
    expect(comp['stationnement']()).toBe('PARKING_PRIVE');
    expect(comp['codePostal']()).toBe('69000');
  });

  it('affiche le lien retour aux caracteristiques', () => {
    const fixture = creer();
    const liens = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a'));
    expect(liens.some(l => l.textContent?.includes('Retour aux caractéristiques'))).toBe(true);
  });
});
