import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { DevisAutoInformations } from './devis-auto-informations';
import { DevisAutoService } from '../../core/service/devis-auto.service';

describe('DevisAutoInformations', () => {
  let fixture: ComponentFixture<DevisAutoInformations>;
  let svc: DevisAutoService;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DevisAutoInformations],
      providers: [
        provideRouter([{ path: 'devis-auto/paiement', redirectTo: '' }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    svc = TestBed.inject(DevisAutoService);
    svc.devisAutoId.set(42);

    fixture = TestBed.createComponent(DevisAutoInformations);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function init(reponse: object | null = null): void {
    fixture.detectChanges();
    const req = http.expectOne(r => r.url.includes('/souscripteur'));
    req.flush(reponse ?? { civilite: null, nom: null, prenom: null, dateNaissance: null, telephone: null, adresse: null, codePostal: null, ville: null });
    fixture.detectChanges();
  }

  it('affiche le formulaire avec les 8 champs', () => {
    init();
    const form = fixture.nativeElement.querySelector('form');
    expect(form).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#nom')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#adresse')).toBeTruthy();
  });

  it('pré-remplit les champs depuis la réponse du serveur', () => {
    init({ civilite: 'madame', nom: 'Durand', prenom: 'Marie', dateNaissance: '1990-01-31', telephone: '0612345678', adresse: '12 rue de la Paix', codePostal: '75001', ville: 'Paris' });

    const nom = fixture.nativeElement.querySelector('#nom') as HTMLInputElement;
    expect(nom.value).toBe('Durand');
    const adresse = fixture.nativeElement.querySelector('#adresse') as HTMLInputElement;
    expect(adresse.value).toBe('12 rue de la Paix');
  });

  it('affiche les erreurs après tentative de soumission sans saisie', () => {
    init();
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const erreurs = fixture.nativeElement.querySelectorAll('.erreur');
    expect(erreurs.length).toBeGreaterThan(0);
  });

  it('envoie la demande au serveur quand le formulaire est valide', () => {
    init({ civilite: 'madame', nom: 'Durand', prenom: 'Marie', dateNaissance: '1990-01-31', telephone: '0612345678', adresse: '12 rue de la Paix', codePostal: '75001', ville: 'Paris' });

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const req = http.expectOne(r => r.url.includes('/souscripteur') && r.method === 'POST');
    expect(req.request.body.civilite).toBe('madame');
    expect(req.request.body.codePostal).toBe('75001');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
