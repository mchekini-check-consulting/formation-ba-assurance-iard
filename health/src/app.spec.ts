import { afterEach, describe, expect, it, vi } from 'vitest';
import { demarrer, type Session } from './app';

// Fausse session keycloak-js : utilisateur connecté, token valide
function session(surcharge: Partial<Session> = {}): Session {
  return {
    token: 'tok',
    tokenParsed: { sub: 'id-1', preferred_username: 'alice' },
    updateToken: vi.fn().mockResolvedValue(false),
    login: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
    ...surcharge,
  } as Session;
}

function repondre(code: number, corps: unknown) {
  return vi.fn().mockResolvedValue(new Response(JSON.stringify(corps), { status: code }));
}

const racine = () => document.body;

afterEach(() => {
  document.body.innerHTML = '';
});

describe('demarrer', () => {
  it("affiche l'utilisateur et l'état UP après la première vérification", async () => {
    const s = session();
    const fetchFn = repondre(200, { status: 'UP' });
    await demarrer(racine(), s, '/sante', fetchFn);
    expect(document.querySelector('.utilisateur')?.textContent).toBe('alice');
    expect(document.querySelector('.libelle')?.textContent).toBe('UP');
    expect((document.querySelector('.etat') as HTMLElement).dataset['etat']).toBe('up');
    expect(document.querySelector('.detail')?.textContent).toContain('HTTP 200');
    expect(s.updateToken).toHaveBeenCalledWith(30);
    expect(fetchFn).toHaveBeenCalledWith('/sante', { headers: { Authorization: 'Bearer tok' } });
  });

  it('affiche le sub sans preferred_username et REFUSE sur un 401', async () => {
    await demarrer(racine(), session({ tokenParsed: { sub: 'id-1' } }), '/sante', repondre(401, {}));
    expect(document.querySelector('.utilisateur')?.textContent).toBe('id-1');
    expect(document.querySelector('.libelle')?.textContent).toBe('REFUSE');
  });

  it('relance une vérification au clic sur Vérifier', async () => {
    const fetchFn = vi
      .fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ status: 'UP' }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ status: 'DOWN' }), { status: 503 }));
    await demarrer(racine(), session(), '/sante', fetchFn);
    (document.querySelector('.verifier') as HTMLButtonElement).click();
    await vi.waitFor(() =>
      expect(document.querySelector('.libelle')?.textContent).toBe('DOWN'),
    );
    expect(fetchFn).toHaveBeenCalledTimes(2);
  });

  it("n'affiche pas de code HTTP quand le gateway est injoignable", async () => {
    const fetchFn = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
    await demarrer(racine(), session({ token: undefined }), '/sante', fetchFn);
    expect(document.querySelector('.detail')?.textContent).not.toContain('HTTP');
    expect(fetchFn).toHaveBeenCalledWith('/sante', { headers: { Authorization: 'Bearer ' } });
  });

  it('renvoie vers la connexion Keycloak si le token ne peut plus être rafraîchi', async () => {
    const s = session({ updateToken: vi.fn().mockRejectedValue(new Error('expirée')) });
    const fetchFn = vi.fn();
    await demarrer(racine(), s, '/sante', fetchFn);
    expect(s.login).toHaveBeenCalled();
    expect(fetchFn).not.toHaveBeenCalled();
  });

  it('se déconnecte et revient sur la page', async () => {
    const s = session();
    await demarrer(racine(), s, '/sante', repondre(200, { status: 'UP' }));
    (document.querySelector('.deconnexion') as HTMLButtonElement).click();
    expect(s.logout).toHaveBeenCalledWith({ redirectUri: location.origin });
  });
});
