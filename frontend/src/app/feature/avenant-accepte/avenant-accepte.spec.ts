import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AVENANT_A03 } from '../../core/model/avenant.exemples';
import { FICHE_AUTO } from '../../core/model/contrat.exemples';
import { AvenantAccepte } from './avenant-accepte';

const ACCEPTE = { ...AVENANT_A03, statut: 'ACCEPTE' as const, accepteLe: '2026-11-26T14:05:00Z', documentId: 70 };
const FICHE = {
  ...FICHE_AUTO,
  numero: 'AE-RECETTE-A03',
  cotisationMensuelle: 48.5,
  garanties: [
    { code: 'RC', prixMensuelTtc: 12 },
    { code: 'ASSISTANCE_0KM', prixMensuelTtc: 3.5 },
  ],
  avenants: [ACCEPTE],
};

describe('AvenantAccepte (US-23, AC4, AC7)', () => {
  let fixture: ComponentFixture<AvenantAccepte>;
  let page: HTMLElement;
  let http: HttpTestingController;

  const ouvrir = (etat: unknown): void => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ numero: 'AE-RECETTE-A03' })) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'currentNavigation').mockReturnValue(
      (etat ? { extras: { state: { acceptation: etat } } } : null) as never,
    );
    fixture = TestBed.createComponent(AvenantAccepte);
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    http.expectOne('/api/assurance-iard-api/contrats/AE-RECETTE-A03').flush(FICHE);
    fixture.detectChanges();
  };

  afterEach(() => http.verify());

  const texte = (): string => page.textContent?.replace(/\s+/g, ' ') ?? '';

  it('AC4 : confirme l’avenant, la date et l’heure d’acceptation, la nouvelle cotisation et les garanties', () => {
    ouvrir({ avenant: ACCEPTE, disponible: true, emailEnvoye: true, versionConditionsParticulieres: 2 });

    expect(page.querySelector('h1')?.textContent).toContain('Avenant accepté');
    expect(texte()).toContain('Votre avenant AV-2026-000009 est accepté');
    expect(texte()).toContain('prennent effet le 27/11/2026');
    expect(texte()).toContain('le 26/11/2026 à 15:05');
    expect(texte()).toContain('48,50 € TTC / mois (au lieu de 47,50 € TTC)');
    expect(texte()).toContain('Assistance dès 0 km');
    expect(texte()).toContain('Nous vous avons envoyé votre avenant par e-mail.');
    // AC7 : la version en vigueur des conditions particulières.
    expect(texte()).toContain('en vigueur, à jour de cet avenant');
  });

  it('AC4 : l’avenant se télécharge', () => {
    ouvrir(null);
    const telecharger = Array.from(page.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Télécharger mon avenant'),
    )!;
    telecharger.click();
    http.expectOne('/api/assurance-iard-api/documents-contrat/70').flush(null, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(texte()).toContain('Document en préparation, réessayez dans quelques minutes.');
    // Rouverte sans le résultat de l'acceptation : pas de message d'envoi inventé.
    expect(texte()).not.toContain('envoyé votre avenant par e-mail');
  });

  it('PDF encore en préparation : le client le sait', () => {
    ouvrir({ avenant: ACCEPTE, disponible: false, emailEnvoye: false, versionConditionsParticulieres: null });
    expect(texte()).toContain('Votre avenant est en préparation');
  });

  it('envoi par e-mail en échec : invite à télécharger', () => {
    ouvrir({ avenant: ACCEPTE, disponible: true, emailEnvoye: false, versionConditionsParticulieres: 2 });
    expect(texte()).toContain("L'envoi par e-mail n'a pas abouti");
  });
});
