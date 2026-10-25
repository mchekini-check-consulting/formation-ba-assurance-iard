import type { LoggerService } from '@nestjs/common';

// Logs en JSON, une ligne par entrée, sur stdout : collectés par la plateforme
// (Alloy -> Loki, dashboard « Logs — Métier »).
// Seules les lignes avec un champ layer (controller | service | db) sont gardées
// par la collecte : le reste (démarrage Nest, routes...) reste visible avec
// kubectl logs mais n'arrive pas dans Loki.
// Niveau minimal : LOG_LEVEL (debug | info | warn | error), injecté par helm.yaml.

export type Level = 'debug' | 'info' | 'warn' | 'error';
export type Layer = 'controller' | 'service' | 'db';

export interface BusinessLog {
  layer: Layer;
  class: string;
  method: string;
  durationMs?: number;
  [field: string]: unknown;
}

const ORDER: Record<Level, number> = { debug: 0, info: 1, warn: 2, error: 3 };

function minLevel(): Level {
  const level = (process.env.LOG_LEVEL ?? 'info').toLowerCase();
  return level in ORDER ? (level as Level) : 'info';
}

export class JsonLogger implements LoggerService {
  private readonly min = ORDER[minLevel()];

  // Entrée métier : layer, class, method obligatoires
  business(level: Level, msg: string, fields: BusinessLog): void {
    this.write(level, msg, fields);
  }

  // Interface LoggerService : logs du framework (context = composant Nest)
  log(message: unknown, context?: string): void {
    this.framework('info', message, context);
  }

  error(message: unknown, stackOrContext?: string, context?: string): void {
    // Nest passe (message, stack, context) ou (message, context)
    const stack = context ? stackOrContext : undefined;
    this.framework('error', message, context ?? stackOrContext, stack);
  }

  warn(message: unknown, context?: string): void {
    this.framework('warn', message, context);
  }

  debug(message: unknown, context?: string): void {
    this.framework('debug', message, context);
  }

  verbose(message: unknown, context?: string): void {
    this.framework('debug', message, context);
  }

  fatal(message: unknown, context?: string): void {
    this.framework('error', message, context);
  }

  private framework(
    level: Level,
    message: unknown,
    context?: string,
    stack?: string,
  ): void {
    const msg = message instanceof Error ? message.message : String(message);
    this.write(level, msg, { context, stack });
  }

  private write(level: Level, msg: string, fields: object): void {
    if (ORDER[level] < this.min) {
      return;
    }
    const line = { time: new Date().toISOString(), level, msg, ...fields };
    process.stdout.write(`${JSON.stringify(line)}\n`);
  }
}

// Instance unique : les décorateurs et le logger TypeORM ne passent pas par
// l'injection de dépendances de Nest
export const logger = new JsonLogger();

// Message d'une erreur, sans les données de la requête
export function errorFields(error: unknown): Record<string, string> {
  if (error instanceof Error) {
    return { error: error.name, errorMessage: error.message };
  }
  return { error: String(error) };
}
