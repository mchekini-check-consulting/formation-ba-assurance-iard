import { AnnonceService } from './annonce.service';

describe('AnnonceService', () => {
  it("ne donne le message qu'une seule fois", () => {
    const service = new AnnonceService();
    expect(service.prendre()).toBeNull();

    service.preparer('Vous êtes déconnecté.');

    expect(service.prendre()).toBe('Vous êtes déconnecté.');
    expect(service.prendre()).toBeNull();
  });
});
