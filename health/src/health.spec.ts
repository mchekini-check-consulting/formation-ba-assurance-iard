import { describe, expect, it, vi } from 'vitest';
import { verifierSante } from './health';

// fetch simulé : réponse JSON avec un code HTTP
function repondre(code: number, corps: unknown = {}) {
  return vi.fn().mockResolvedValue(new Response(JSON.stringify(corps), { status: code }));
}

describe('verifierSante', () => {
  it('envoie le token en Bearer et renvoie UP', async () => {
    const fetchFn = repondre(200, { status: 'UP' });
    const sante = await verifierSante('/sante', 'abc', fetchFn);
    expect(fetchFn).toHaveBeenCalledWith('/sante', { headers: { Authorization: 'Bearer abc' } });
    expect(sante).toEqual({ etat: 'UP', code: 200, detail: 'Backend opérationnel' });
  });

  it('renvoie DOWN quand le backend est en difficulté', async () => {
    expect((await verifierSante('/sante', 't', repondre(503, { status: 'DOWN' }))).etat).toBe(
      'DOWN',
    );
  });

  it('renvoie DOWN sur un 503 sans corps JSON', async () => {
    const fetchFn = vi.fn().mockResolvedValue(new Response('indisponible', { status: 503 }));
    expect((await verifierSante('/sante', 't', fetchFn)).etat).toBe('DOWN');
  });

  it('renvoie REFUSE sur un 401 du gateway', async () => {
    expect(await verifierSante('/sante', 't', repondre(401))).toEqual({
      etat: 'REFUSE',
      code: 401,
      detail: 'Token refusé par le gateway',
    });
  });

  it('renvoie REFUSE sur un 403 (token d’un autre client)', async () => {
    const sante = await verifierSante('/sante', 't', repondre(403));
    expect(sante.etat).toBe('REFUSE');
    expect(sante.code).toBe(403);
  });

  it('renvoie ERREUR quand le gateway est injoignable', async () => {
    const fetchFn = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
    expect(await verifierSante('/sante', 't', fetchFn)).toEqual({
      etat: 'ERREUR',
      code: 0,
      detail: 'Gateway injoignable',
    });
  });

  it('renvoie ERREUR sur une réponse inattendue', async () => {
    expect((await verifierSante('/sante', 't', repondre(200, { autre: 1 }))).etat).toBe('ERREUR');
  });
});
