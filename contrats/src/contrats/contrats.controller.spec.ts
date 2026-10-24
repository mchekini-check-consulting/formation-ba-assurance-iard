import { ContratsController } from './contrats.controller.js';
import { ContratsService } from './contrats.service.js';

describe('ContratsController', () => {
  const service = { lister: vi.fn(), creer: vi.fn() };
  const controller = new ContratsController(
    service as unknown as ContratsService,
  );

  it('délègue la liste et la création au service', async () => {
    const contrat = { id: '1', numero: 'C1', assure: 'a' };
    service.lister.mockResolvedValue([]);
    service.creer.mockResolvedValue(contrat);
    await expect(controller.lister()).resolves.toEqual([]);
    await expect(
      controller.creer({ numero: 'C1', assure: 'a' }),
    ).resolves.toEqual(contrat);
    expect(service.creer).toHaveBeenCalledWith({ numero: 'C1', assure: 'a' });
  });
});
