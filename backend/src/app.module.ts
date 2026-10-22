import { Module } from '@nestjs/common';
import { AppController } from './app.controller.js';
import { AppService } from './app.service.js';
import { DatabaseModule } from './database/database.module.js';
import { ItemsModule } from './items/items.module.js';

@Module({
  imports: [DatabaseModule, ItemsModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
