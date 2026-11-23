import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { DocumentClientPage } from './document-client';

describe('DocumentClientPage (US-04)', () => {
  let fixture: ComponentFixture<DocumentClientPage>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:document');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '5' }) } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DocumentClientPage);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
  });

  it('prépare le document puis libère sa mémoire en quittant la page', () => {
    expect(page.textContent).toContain('Chargement du document');
    http.expectOne('/api/assurance-iard-api/documents/5').flush(new Blob(['pdf']));
    fixture.detectChanges();

    expect(page.querySelector('main .btn')?.getAttribute('href')).toBe('blob:document');
    fixture.destroy();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:document');
  });

  it("affiche un message clair si le document n'a pas pu être ouvert", () => {
    http.expectOne('/api/assurance-iard-api/documents/5').flush(new Blob(), { status: 500, statusText: 'x' });
    fixture.detectChanges();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain("n'a pas pu être ouvert");
    fixture.destroy();
    expect(URL.revokeObjectURL).not.toHaveBeenCalled();
  });
});
