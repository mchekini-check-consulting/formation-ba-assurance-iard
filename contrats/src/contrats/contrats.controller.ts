import { Body, Controller, Get, Post } from '@nestjs/common';
import { Contrat } from './contrat.entity.js';
import { ContratsService } from './contrats.service.js';
import type { ContratRequest } from './contrats.service.js';

// GET /contrats : liste ; POST /contrats {numero, assure} : création (les deux obligatoires).
// Exposé par la passerelle :
// https://gateway.formation-ba-assurance-iard.<env>.check-consulting.net/contrats/contrats
@Controller('contrats')
export class ContratsController {
  constructor(private readonly contratsService: ContratsService) {}

  @Get()
  lister(): Promise<Contrat[]> {
    return this.contratsService.lister();
  }

  @Post()
  creer(@Body() requete: ContratRequest): Promise<Contrat> {
    return this.contratsService.creer(requete);
  }
}
