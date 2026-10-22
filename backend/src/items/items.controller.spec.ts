import { ItemsController } from './items.controller.js';
import { ItemsService } from './items.service.js';

describe('ItemsController', () => {
  const service = { lister: vi.fn(), creer: vi.fn() };
  const controller = new ItemsController(service as unknown as ItemsService);

  it('délègue la liste et la création au service', async () => {
    service.lister.mockResolvedValue([]);
    service.creer.mockResolvedValue({ id: '1', nom: 'a' });
    await expect(controller.lister()).resolves.toEqual([]);
    await expect(controller.creer({ nom: 'a' })).resolves.toEqual({
      id: '1',
      nom: 'a',
    });
    expect(service.creer).toHaveBeenCalledWith({ nom: 'a' });
  });
});
