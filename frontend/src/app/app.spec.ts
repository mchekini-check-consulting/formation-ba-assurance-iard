import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { App } from './app';
import { HelloService } from './core/service/hello.service';

function creerApp(reponse: Observable<string>) {
  TestBed.configureTestingModule({
    imports: [App],
    providers: [provideRouter([]), { provide: HelloService, useValue: { direBonjour: () => reponse } }],
  });
  const fixture = TestBed.createComponent(App);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('App', () => {
  it('affiche le titre', () => {
    const page = creerApp(of('Hello Assurance IARD'));
    expect(page.querySelector('h1')?.textContent).toContain('Assurance IARD');
  });

  it('affiche le message du backend', () => {
    const page = creerApp(of('Hello Assurance IARD'));
    expect(page.querySelector('.message')?.textContent).toContain('Hello Assurance IARD');
  });

  it("affiche un message d'erreur si le backend ne répond pas", () => {
    const page = creerApp(throwError(() => new Error('réseau')));
    expect(page.querySelector('.erreur')?.textContent).toContain('Le serveur ne répond pas');
  });
});
