import { Module } from '@nestjs/common';
import { AppController } from './app.controller.js';
import { AppService } from './app.service.js';
import { ContratsModule } from './contrats/contrats.module.js';
import { DatabaseModule } from './database/database.module.js';

@Module({
  imports: [DatabaseModule, ContratsModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
