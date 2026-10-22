import { Body, Controller, Get, Post } from '@nestjs/common';
import { Item } from './item.entity.js';
import { ItemsService } from './items.service.js';
import type { ItemRequest } from './items.service.js';

// GET /items : liste ; POST /items {nom, description} : création (nom obligatoire).
// Exposé par la passerelle :
// https://api.<env>.check-consulting.net/formation-ba-assurance-iard/backend/items
@Controller('items')
export class ItemsController {
  constructor(private readonly itemsService: ItemsService) {}

  @Get()
  lister(): Promise<Item[]> {
    return this.itemsService.lister();
  }

  @Post()
  creer(@Body() requete: ItemRequest): Promise<Item> {
    return this.itemsService.creer(requete);
  }
}
