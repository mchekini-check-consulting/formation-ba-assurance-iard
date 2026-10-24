import { BadRequestException } from '@nestjs/common';
import { Repository } from 'typeorm';
import { Contrat } from './contrat.entity.js';
import { ContratsService } from './contrats.service.js';

describe('ContratsService', () => {
  const repository = {
    find: vi.fn(),
    create: vi.fn((contrat: Partial<Contrat>) => contrat),
    save: vi.fn((contrat: Partial<Contrat>) =>
      Promise.resolve({ id: '1', ...contrat }),
    ),
  };
  const service = new ContratsService(
    repository as unknown as Repository<Contrat>,
  );

  beforeEach(() => vi.clearAllMocks());

  it('liste les contrats par id', async () => {
    repository.find.mockResolvedValue([{ id: '1', numero: 'C1' }]);
    await expect(service.lister()).resolves.toEqual([
      { id: '1', numero: 'C1' },
    ]);
    expect(repository.find).toHaveBeenCalledWith({ order: { id: 'ASC' } });
  });

  it('crée un contrat nettoyé', async () => {
    const cree = await service.creer({
      numero: '  CTR-001  ',
      assure: '  Jean Dupont ',
    });
    expect(cree).toEqual({ id: '1', numero: 'CTR-001', assure: 'Jean Dupont' });
  });

  it('refuse un numéro vide', () => {
    expect(() => service.creer({ numero: ' ', assure: 'x' })).toThrow(
      BadRequestException,
    );
    expect(repository.save).not.toHaveBeenCalled();
  });

  it('refuse un contrat sans numéro', () => {
    expect(() => service.creer({ assure: 'x' })).toThrow(BadRequestException);
    expect(repository.save).not.toHaveBeenCalled();
  });

  it('refuse un assuré vide', () => {
    expect(() => service.creer({ numero: 'C1' })).toThrow(BadRequestException);
    expect(repository.save).not.toHaveBeenCalled();
  });
});
