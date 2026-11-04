import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Recommandation, ReponsesBesoins } from '../model/recommandation';
import { RecommandationService } from './recommandation.service';

describe('RecommandationService', () => {
  const reponses: ReponsesBesoins = {
    valeurContenu: 15000,
    objetsValeur: true,
    appareilsCouteux: false,
    remboursement: 'vetuste',
    assistance: false,
    litiges: false,
    priorite: 'equilibre',
  };
  const recommandation: Recommandation = {
    formule: 'confort',
    raisons: ['Vous déclarez des objets de valeur'],
    options: ['objets-valeur'],
  };

  let service: RecommandationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(RecommandationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envoie les réponses et la recommandation, puis renvoie la référence', () => {
    let resultat: string | null | undefined;
    service.enregistrer(reponses, recommandation).subscribe((r) => (resultat = r));
    const requete = http.expectOne('/api/devis/recommandations');
    expect(requete.request.method).toBe('POST');
    expect(requete.request.body).toEqual({ ...reponses, ...recommandation });
    requete.flush({ reference: 'ref-1' }, { status: 201, statusText: 'Created' });

    expect(resultat).toBe('ref-1');
  });

  it('renvoie null en cas d’échec', () => {
    let resultat: string | null | undefined;
    service.enregistrer(reponses, recommandation).subscribe((r) => (resultat = r));
    http.expectOne('/api/devis/recommandations').flush(null, { status: 500, statusText: 'Erreur' });

    expect(resultat).toBeNull();
  });
});
