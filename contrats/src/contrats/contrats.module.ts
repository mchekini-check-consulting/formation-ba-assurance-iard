import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Contrat } from './contrat.entity.js';
import { ContratsController } from './contrats.controller.js';
import { ContratsService } from './contrats.service.js';

@Module({
  imports: [TypeOrmModule.forFeature([Contrat])],
  controllers: [ContratsController],
  providers: [ContratsService],
})
export class ContratsModule {}
