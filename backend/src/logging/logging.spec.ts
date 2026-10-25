import { BadRequestException } from '@nestjs/common';
import { JsonLogger, logger } from './json-logger.js';
import { Loggable } from './loggable.decorator.js';
import { TypeOrmLogger } from './typeorm-logger.js';

// Lignes JSON écrites sur stdout pendant fn
function capture(fn: () => unknown): Promise<Record<string, unknown>[]> {
  const lines: string[] = [];
  const spy = vi
    .spyOn(process.stdout, 'write')
    .mockImplementation((chunk: string | Uint8Array) => {
      lines.push(String(chunk));
      return true;
    });
  return Promise.resolve()
    .then(fn)
    .catch(() => undefined)
    .then(() => {
      spy.mockRestore();
      return lines.map((line) => JSON.parse(line) as Record<string, unknown>);
    });
}

@Loggable()
class ExempleService {
  facteur = 2;

  doubler(n: number): number {
    return n * this.facteur;
  }

  async charger(): Promise<string> {
    return 'ok';
  }

  refuser(): never {
    throw new BadRequestException('nom obligatoire');
  }

  async planter(): Promise<never> {
    throw new Error('connexion perdue');
  }
}

describe('logging', () => {
  const service = new ExempleService();

  afterEach(() => {
    delete process.env.LOG_LEVEL;
  });

  it('écrit une ligne métier JSON avec layer, class et method', async () => {
    const [line] = await capture(() =>
      logger.business('info', 'GET /items 200', {
        layer: 'controller',
        class: 'ItemsController',
        method: 'lister',
        durationMs: 3,
      }),
    );
    expect(line).toMatchObject({
      level: 'info',
      msg: 'GET /items 200',
      layer: 'controller',
      class: 'ItemsController',
      method: 'lister',
      durationMs: 3,
    });
    expect(typeof line.time).toBe('string');
  });

  it("n'ajoute pas de layer aux logs du framework (filtrés à la collecte)", async () => {
    const [line] = await capture(() =>
      logger.log('Mapped {/items, GET} route', 'RouterExplorer'),
    );
    expect(line).toMatchObject({ level: 'info', context: 'RouterExplorer' });
    expect(line).not.toHaveProperty('layer');
  });

  it('respecte LOG_LEVEL', async () => {
    process.env.LOG_LEVEL = 'warn';
    const filtre = new JsonLogger();
    const lines = await capture(() => {
      filtre.log('info ignoré');
      filtre.warn('avertissement gardé');
    });
    expect(lines.map((line) => line.msg)).toEqual(['avertissement gardé']);
  });

  it('@Loggable : méthode synchrone, this conservé, durée mesurée', async () => {
    let result = 0;
    const [line] = await capture(() => (result = service.doubler(21)));
    expect(result).toBe(42);
    expect(line).toMatchObject({
      level: 'info',
      layer: 'service',
      class: 'ExempleService',
      method: 'doubler',
    });
    expect(line.durationMs).toBeGreaterThanOrEqual(0);
  });

  it('@Loggable : méthode asynchrone loggée à la résolution', async () => {
    const [line] = await capture(() => service.charger());
    expect(line).toMatchObject({ level: 'info', method: 'charger' });
  });

  it('@Loggable : règle métier (4xx) en warn, erreur relancée', async () => {
    expect(() => service.refuser()).toThrow(BadRequestException);
    const [line] = await capture(() => service.refuser());
    expect(line).toMatchObject({
      level: 'warn',
      method: 'refuser',
      error: 'BadRequestException',
      errorMessage: 'nom obligatoire',
    });
  });

  it('@Loggable : erreur inattendue en error', async () => {
    await expect(service.planter()).rejects.toThrow('connexion perdue');
    const [line] = await capture(() => service.planter());
    expect(line).toMatchObject({ level: 'error', method: 'planter' });
  });

  it('TypeORM : requêtes lentes et en erreur seulement, sans paramètres', async () => {
    const typeorm = new TypeOrmLogger();
    const lines = await capture(() => {
      typeorm.logQuery();
      typeorm.logQuerySlow(450, 'SELECT * FROM item');
      typeorm.logQueryError(new Error('relation absente'), 'SELECT 1');
    });
    expect(lines).toHaveLength(2);
    expect(lines[0]).toMatchObject({
      level: 'warn',
      layer: 'db',
      durationMs: 450,
      query: 'SELECT * FROM item',
    });
    expect(lines[1]).toMatchObject({
      level: 'error',
      layer: 'db',
      errorMessage: 'relation absente',
    });
    expect(lines[0]).not.toHaveProperty('parameters');
  });
});
