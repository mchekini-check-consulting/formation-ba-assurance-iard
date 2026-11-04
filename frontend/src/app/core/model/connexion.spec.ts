import { codeValide, pageDeRetour } from './connexion';

describe('codeValide', () => {
  it.each([
    ['123456', true],
    ['000000', true],
    ['12345', false],
    ['1234567', false],
    ['12a456', false],
    ['', false],
  ])('%s -> %s', (code, attendu) => {
    expect(codeValide(code)).toBe(attendu);
  });
});

describe('pageDeRetour', () => {
  it.each([
    ['/espace-client/documents', '/espace-client/documents'],
    ['/espace-client?onglet=2', '/espace-client?onglet=2'],
    [null, '/espace-client'],
    [undefined, '/espace-client'],
    ['', '/espace-client'],
    ['https://pirate.example', '/espace-client'],
    ['//pirate.example', '/espace-client'],
    ['/\\pirate.example', '/espace-client'],
  ])('%s -> %s', (retour, attendu) => {
    expect(pageDeRetour(retour)).toBe(attendu);
  });
});
