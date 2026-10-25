import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module.js';
import { logger } from './logging/json-logger.js';
import { LoggingInterceptor } from './logging/logging.interceptor.js';

async function bootstrap() {
  // Logs JSON sur stdout (src/logging) ; bufferLogs : les logs du démarrage
  // passent aussi par ce logger
  const app = await NestFactory.create(AppModule, { bufferLogs: true });
  app.useLogger(logger);
  app.useGlobalInterceptors(new LoggingInterceptor());
  await app.listen(process.env.PORT ?? 3000);
}
await bootstrap();
