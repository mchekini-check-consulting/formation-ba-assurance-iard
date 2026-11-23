import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { DevisAutoVehicule } from './devis-auto-vehicule';

const fournirRoute = (mode: string | null) => ({
  provide: ActivatedRoute,
  useValue: { snapshot: { queryParamMap: { get: (key: string) => (key === 'mode' ? mode : null) } } },
});

describe('DevisAutoVehicule', () => {
  beforeEach(() => history.pushState({}, ''));

  it('affiche "(aucun mode reçu)" quand aucun mode n\'est transmis', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), fournirRoute(null)] });
    const fixture = TestBed.createComponent(DevisAutoVehicule);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('aucun mode reçu');
  });

  it('affiche "plaque" depuis le paramètre URL ?mode=plaque', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), fournirRoute('plaque')] });
    const fixture = TestBed.createComponent(DevisAutoVehicule);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('plaque');
  });

  it('affiche "marque" depuis le paramètre URL ?mode=marque', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), fournirRoute('marque')] });
    const fixture = TestBed.createComponent(DevisAutoVehicule);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('marque');
  });

  it('affiche "plaque" depuis l\'état de navigation (history.state)', () => {
    history.pushState({ mode: 'plaque' }, '');
    TestBed.configureTestingModule({ providers: [provideRouter([]), fournirRoute(null)] });
    const fixture = TestBed.createComponent(DevisAutoVehicule);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('plaque');
  });

  it('ignore un mode invalide dans le paramètre URL', () => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), fournirRoute('invalide')] });
    const fixture = TestBed.createComponent(DevisAutoVehicule);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('aucun mode reçu');
  });
});
