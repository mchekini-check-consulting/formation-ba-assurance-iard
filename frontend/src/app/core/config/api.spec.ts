import { apiBase } from './api';

describe('apiBase', () => {
  it("pointe la passerelle de l'environnement du frontend", () => {
    expect(
      apiBase('backend', 'frontend.formation-ba-assurance-iard.qualif.check-consulting.net'),
    ).toBe('https://gateway.formation-ba-assurance-iard.qualif.check-consulting.net/backend');
  });

  it('utilise le nom du backend demandé', () => {
    expect(
      apiBase('contrats', 'frontend.formation-ba-assurance-iard.dev.check-consulting.net'),
    ).toBe('https://gateway.formation-ba-assurance-iard.dev.check-consulting.net/contrats');
  });

  it('utilise le proxy du serveur de dev en local', () => {
    expect(apiBase('backend', 'localhost')).toBe('/api');
  });

  it("prend par défaut l'adresse de la page", () => {
    // Les tests tournent sur localhost : proxy local
    expect(apiBase('backend')).toBe('/api');
  });
});
