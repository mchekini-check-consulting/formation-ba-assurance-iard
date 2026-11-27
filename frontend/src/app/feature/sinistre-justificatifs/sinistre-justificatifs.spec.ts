import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { DeclarationSinistre, MESSAGE_FORMAT, MESSAGE_TAILLE, TEXTE_HONNEUR } from '../../core/model/sinistre';
import { BROUILLON_COMPLET } from '../../core/model/sinistre.exemples';
import { MESSAGE_STOCKAGE } from '../../core/service/sinistre.service';
import { MESSAGE_HONNEUR, SinistreJustificatifs } from './sinistre-justificatifs';

const ADRESSE = '/api/assurance-iard-api/sinistres/42';
const AVEC_PIECE: DeclarationSinistre = {
  ...BROUILLON_COMPLET,
  piecesManquantes: false,
  piecesJointes: [
    {
      id: 7,
      typePiece: 'RECEPISSE_PLAINTE',
      nomFichier: 'recepisse.pdf',
      format: 'PDF',
      tailleOctets: 2048,
      deposeeLe: '2026-11-26T10:00:00Z',
    },
  ],
};

describe('SinistreJustificatifs, étape 3 (US-24, AC7, AC9, AC14 à AC16, RG8)', () => {
  let fixture: ComponentFixture<SinistreJustificatifs>;
  let page: HTMLElement;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '42' })) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(SinistreJustificatifs);
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const charger = (declaration: DeclarationSinistre = BROUILLON_COMPLET): void => {
    http.expectOne(ADRESSE).flush(declaration);
    fixture.detectChanges();
  };
  const bouton = (texte: string): HTMLButtonElement =>
    Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes(texte))!;
  const deposer = (nom: string, taille: number, index = 0): void => {
    const champ = page.querySelectorAll<HTMLInputElement>('input[type="file"]')[index];
    const fichier = new File([new Uint8Array(1)], nom);
    Object.defineProperty(fichier, 'size', { value: taille });
    Object.defineProperty(champ, 'files', { value: [fichier], configurable: true });
    champ.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  it('AC7 : pièces attendues, obligatoires et facultatives ; dépôt ou photo sur mobile ; envoi possible plus tard', () => {
    charger();
    const pieces = Array.from(page.querySelectorAll('.piece')).map((p) => p.textContent?.replace(/\s+/g, ' '));
    expect(pieces[0]).toContain('Récépissé de dépôt de plainte');
    expect(pieces[0]).toContain('Obligatoire');
    expect(pieces[1]).toContain('Facultative');
    const photo = page.querySelectorAll<HTMLInputElement>('input[type="file"]')[1];
    expect(photo.getAttribute('capture')).toBe('environment');
    expect(page.querySelector<HTMLInputElement>('input[type="file"]')?.accept).toContain('.pdf');
    expect(page.textContent).toContain('déposer les justificatifs manquants plus tard');
  });

  it('AC15 : un fichier trop lourd ou au mauvais format est refusé avec la raison, sans appel', () => {
    charger();
    deposer('constat.pdf', 12 * 1024 * 1024);
    expect(page.querySelector('.piece [role="alert"]')?.textContent).toBe(MESSAGE_TAILLE);
    deposer('facture.exe', 1000);
    expect(page.querySelector('.piece [role="alert"]')?.textContent).toBe(MESSAGE_FORMAT);
  });

  it('AC14 : une pièce stockée s’affiche avec son nom, une coche verte et « Supprimer »', () => {
    charger();
    deposer('recepisse.pdf', 2048);
    const depot = http.expectOne(`${ADRESSE}/pieces`);
    expect((depot.request.body as FormData).get('typePiece')).toBe('RECEPISSE_PLAINTE');
    depot.flush(AVEC_PIECE);
    fixture.detectChanges();

    const fichier = page.querySelector('.piece__fichiers li')!;
    expect(fichier.textContent).toContain('recepisse.pdf');
    expect(fichier.querySelector('.piece__coche')?.textContent).toBe('✓');
    expect(fichier.textContent).toContain('Supprimer');
    expect(page.textContent).not.toContain('déposer les justificatifs manquants plus tard');

    bouton('Supprimer').click();
    http.expectOne(`${ADRESSE}/pieces/7`).flush(BROUILLON_COMPLET);
    fixture.detectChanges();
    expect(page.querySelector('.piece__fichiers')).toBeNull();
  });

  it('AC16 : stockage indisponible → message pour réessayer ou envoyer plus tard', () => {
    charger();
    deposer('erreur-stockage.pdf', 2048);
    http.expectOne(`${ADRESSE}/pieces`).flush({ detail: 'x' }, { status: 503, statusText: 'x' });
    fixture.detectChanges();
    expect(page.querySelector('.piece [role="alert"]')?.textContent).toBe(MESSAGE_STOCKAGE);
  });

  it('AC9 : récapitulatif complet, dates JJ/MM/AAAA et heure HH:MM', () => {
    charger();
    const recap = page.querySelector('.recap')!.textContent?.replace(/\s+/g, ' ') ?? '';
    expect(recap).toContain('Vol du véhicule');
    expect(recap).toContain('25/11/2026 à 08:30');
    expect(recap).toContain('Parking du travail');
    const termes = Array.from(page.querySelectorAll('.recap dt')).map((dt) => dt.textContent?.trim());
    const plainte = termes.indexOf('Avez-vous déposé plainte ?');
    expect(plainte).toBeGreaterThan(0);
    expect(page.querySelectorAll('.recap dd')[plainte].textContent?.trim()).toBe('Oui');
    expect(recap).toContain('L’assuré lui-même');
    expect(recap).toContain('des pièces obligatoires restent à déposer');
  });

  it('RG8 : la déclaration sur l’honneur n’est jamais pré-cochée ; sans elle, rien n’est envoyé', () => {
    charger();
    const honneur = page.querySelector<HTMLInputElement>('.honneur input')!;
    expect(honneur.checked).toBe(false);
    // RG8 (IA-87), mot pour mot : texte de la case, puis mention d'information sous la case.
    expect(page.querySelector('.honneur')?.textContent?.replace(' *', '').trim()).toBe(
      "Je certifie sur l'honneur que les informations de cette déclaration sont exactes et sincères. Une fausse " +
        'déclaration faite intentionnellement sur la date, les circonstances ou les conséquences du sinistre entraîne ' +
        'la perte de tout droit à indemnité pour ce sinistre (déchéance de garantie prévue aux conditions générales ' +
        'de mon contrat).',
    );
    expect(TEXTE_HONNEUR).toContain('exactes et sincères');
    const mention = page.querySelector('.honneur + .honneur__mention');
    expect(mention?.textContent?.trim()).toBe(
      "La déchéance ne s'applique qu'en cas de fausse déclaration intentionnelle, que l'assureur doit prouver. " +
        'Une simple erreur ou un oubli de bonne foi ne vous prive pas de votre indemnité. Vous pourrez compléter ' +
        'ou corriger votre déclaration auprès de votre gestionnaire.',
    );
    bouton('Envoyer ma déclaration').click();
    fixture.detectChanges();
    expect(page.querySelector('[role="alert"]')?.textContent).toBe(MESSAGE_HONNEUR);
  });

  it('AC9 : cocher puis « Envoyer ma déclaration » ouvre « Déclaration envoyée »', () => {
    charger();
    const honneur = page.querySelector<HTMLInputElement>('.honneur input')!;
    honneur.checked = true;
    honneur.dispatchEvent(new Event('change'));
    bouton('Envoyer ma déclaration').click();
    const envoi = http.expectOne(`${ADRESSE}/envoi`);
    expect(envoi.request.body).toEqual({ declarationHonneur: true });
    const resultat = { id: 42, numero: 'SIN-2026-000042', statut: 'DECLARE', declareLe: '', piecesManquantes: true, emailEnvoye: true };
    envoi.flush(resultat);
    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/sinistres', '42', 'envoyee'], {
      state: { envoi: resultat },
    });
  });

  it.each([
    [400, 'Expliquez en quelques mots la raison du délai de déclaration.'],
    // AC1 (décision PO du 27/11/2026) : brouillon d'un contrat devenu Impayé ou Suspendu.
    [422, "La situation actuelle de votre contrat ne permet plus d'envoyer cette déclaration en ligne. Appelez notre assistance 24h/24 au 01 23 45 67 89 : un conseiller enregistrera votre sinistre avec vous."],
  ])('un refus %i à l’envoi est affiché tel quel', (status, detail) => {
    charger();
    const honneur = page.querySelector<HTMLInputElement>('.honneur input')!;
    honneur.checked = true;
    honneur.dispatchEvent(new Event('change'));
    bouton('Envoyer ma déclaration').click();
    http.expectOne(`${ADRESSE}/envoi`).flush({ detail }, { status, statusText: 'x' });
    fixture.detectChanges();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain(detail);
  });

  it('AC8 : « Enregistrer et terminer plus tard » à l’étape 3', () => {
    charger();
    bouton('Enregistrer et terminer plus tard').click();
    const enregistrement = http.expectOne(ADRESSE);
    expect(enregistrement.request.body.etape).toBe(3);
    enregistrement.flush(BROUILLON_COMPLET);
    expect(router.navigate).toHaveBeenCalledWith(['/espace-client/sinistres'], expect.anything());
  });

  it('AC7 : un dossier envoyé ne montre que ses justificatifs, sans « Supprimer » ni nouvel envoi', () => {
    charger({ ...AVEC_PIECE, statut: 'DECLARE', numero: 'SIN-2026-000042', piecesManquantes: true });
    expect(page.querySelector('h1')?.textContent).toContain('Compléter mes justificatifs');
    expect(page.textContent).toContain('Dossier SIN-2026-000042');
    expect(page.querySelector('.recap')).toBeNull();
    expect(bouton('Supprimer')).toBeUndefined();
  });

  it('ouvre un justificatif déposé', () => {
    charger(AVEC_PIECE);
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:piece');
    const ouvrir = vi.spyOn(window, 'open').mockReturnValue(null);
    bouton('recepisse.pdf').click();
    http.expectOne(`${ADRESSE}/pieces/7`).flush(new Blob(['%PDF']));
    expect(ouvrir).toHaveBeenCalledWith('blob:piece', '_blank', 'noopener');
  });
});
