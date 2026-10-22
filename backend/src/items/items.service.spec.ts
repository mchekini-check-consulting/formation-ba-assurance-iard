import { BadRequestException } from '@nestjs/common';
import { Repository } from 'typeorm';
import { Item } from './item.entity.js';
import { ItemsService } from './items.service.js';

describe('ItemsService', () => {
  const repository = {
    find: vi.fn(),
    create: vi.fn((item: Partial<Item>) => item),
    save: vi.fn((item: Partial<Item>) => Promise.resolve({ id: '1', ...item })),
  };
  const service = new ItemsService(repository as unknown as Repository<Item>);

  beforeEach(() => vi.clearAllMocks());

  it('liste les items par id', async () => {
    repository.find.mockResolvedValue([{ id: '1', nom: 'a' }]);
    await expect(service.lister()).resolves.toEqual([{ id: '1', nom: 'a' }]);
    expect(repository.find).toHaveBeenCalledWith({ order: { id: 'ASC' } });
  });

  it('crée un item nettoyé', async () => {
    const cree = await service.creer({ nom: '  Premier  ', description: '  ' });
    expect(cree).toEqual({ id: '1', nom: 'Premier', description: null });
  });

  it('refuse un nom vide', () => {
    expect(() => service.creer({ nom: ' ' })).toThrow(BadRequestException);
    expect(repository.save).not.toHaveBeenCalled();
  });
});
