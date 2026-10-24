import { BadRequestException, Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Contrat } from './contrat.entity.js';

// Corps de POST /contrats
export interface ContratRequest {
  numero?: string;
  assure?: string;
}

@Injectable()
export class ContratsService {
  constructor(
    @InjectRepository(Contrat) private readonly contrats: Repository<Contrat>,
  ) {}

  lister(): Promise<Contrat[]> {
    return this.contrats.find({ order: { id: 'ASC' } });
  }

  creer(requete: ContratRequest): Promise<Contrat> {
    const numero = (requete.numero ?? '').trim();
    if (!numero) {
      throw new BadRequestException('numero obligatoire');
    }
    const assure = (requete.assure ?? '').trim();
    if (!assure) {
      throw new BadRequestException('assure obligatoire');
    }
    return this.contrats.save(this.contrats.create({ numero, assure }));
  }
}
