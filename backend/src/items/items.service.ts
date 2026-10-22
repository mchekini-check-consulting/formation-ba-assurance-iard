import { BadRequestException, Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Item } from './item.entity.js';

// Corps de POST /items
export interface ItemRequest {
  nom?: string;
  description?: string | null;
}

@Injectable()
export class ItemsService {
  constructor(
    @InjectRepository(Item) private readonly items: Repository<Item>,
  ) {}

  lister(): Promise<Item[]> {
    return this.items.find({ order: { id: 'ASC' } });
  }

  creer(requete: ItemRequest): Promise<Item> {
    const nom = (requete.nom ?? '').trim();
    if (!nom) {
      throw new BadRequestException('nom obligatoire');
    }
    const description = (requete.description ?? '').trim() || null;
    return this.items.save(this.items.create({ nom, description }));
  }
}
