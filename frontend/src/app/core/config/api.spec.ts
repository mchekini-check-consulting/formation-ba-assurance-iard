import { apiBase } from './api';

describe('apiBase', () => {
  it("pointe la passerelle de l'environnement du frontend", () => {
    expect(
      apiBase('assurance-iard-api', 'frontend.formation-ba-assurance-iard.qualif.check-consulting.net'),
    ).toBe('https://gateway.formation-ba-assurance-iard.qualif.check-consulting.net/assurance-iard-api');
  });

  it('utilise le nom du backend demandé', () => {
    expect(
      apiBase('contrats', 'frontend.formation-ba-assurance-iard.dev.check-consulting.net'),
    ).toBe('https://gateway.formation-ba-assurance-iard.dev.check-consulting.net/contrats');
  });

  it('utilise le proxy du serveur de dev en local', () => {
    expect(apiBase('assurance-iard-api', 'localhost')).toBe('/api/assurance-iard-api');
    expect(apiBase('moteurtarifaire', 'localhost')).toBe('/api/moteurtarifaire');
  });

  it("prend par défaut l'adresse de la page", () => {
    // Les tests tournent sur localhost : proxy local
    expect(apiBase('assurance-iard-api')).toBe('/api/assurance-iard-api');
  });
});
