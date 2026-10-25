import type { Logger } from 'typeorm';
import { errorFields, logger } from './json-logger.js';

// Logger TypeORM (layer db) : seulement les requêtes en erreur et les requêtes
// lentes (au-delà de maxQueryExecutionTime, database.module.ts). Les requêtes
// normales ne sont pas loggées (volume), les paramètres jamais (données personnelles).
// Migrations et schéma : logs du framework, sans layer.
export class TypeOrmLogger implements Logger {
  logQuery(): void {}

  logQueryError(error: string | Error, query: string): void {
    logger.business('error', 'requête en erreur', {
      layer: 'db',
      class: 'TypeORM',
      method: 'query',
      query: truncate(query),
      ...errorFields(error),
    });
  }

  logQuerySlow(time: number, query: string): void {
    logger.business('warn', 'requête lente', {
      layer: 'db',
      class: 'TypeORM',
      method: 'query',
      durationMs: time,
      query: truncate(query),
    });
  }

  logSchemaBuild(message: string): void {
    logger.debug(message, 'TypeORM');
  }

  logMigration(message: string): void {
    logger.log(message, 'TypeORM');
  }

  log(level: 'log' | 'info' | 'warn', message: unknown): void {
    if (level === 'warn') {
      logger.warn(message, 'TypeORM');
    } else {
      logger.debug(message, 'TypeORM');
    }
  }
}

function truncate(query: string): string {
  return query.length > 1000 ? `${query.slice(0, 1000)}...` : query;
}
