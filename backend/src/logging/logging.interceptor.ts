import { HttpException } from '@nestjs/common';
import type {
  CallHandler,
  ExecutionContext,
  NestInterceptor,
} from '@nestjs/common';
import type { Request, Response } from 'express';
import { Observable, tap } from 'rxjs';
import { errorFields, logger } from './json-logger.js';

// Un log par appel de contrôleur (layer controller) : route, statut, durée.
// 4xx : warn (erreur du client) ; 5xx et exceptions inattendues : error.
// Enregistré globalement dans main.ts (app.useGlobalInterceptors).
export class LoggingInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
    if (context.getType() !== 'http') {
      return next.handle();
    }
    const http = context.switchToHttp();
    const request = http.getRequest<Request>();
    const fields = {
      layer: 'controller' as const,
      class: context.getClass().name,
      method: context.getHandler().name,
      httpMethod: request.method,
      // Route déclarée (/items/:id), pas l'URL réelle : pas d'identifiants dans les logs
      route:
        (request.route as { path?: string } | undefined)?.path ?? request.path,
    };
    const start = performance.now();
    const duration = () => Math.round(performance.now() - start);

    return next.handle().pipe(
      tap({
        next: () => {
          const status = http.getResponse<Response>().statusCode;
          logger.business(
            'info',
            `${fields.httpMethod} ${fields.route} ${status}`,
            { ...fields, status, durationMs: duration() },
          );
        },
        error: (error: unknown) => {
          const status =
            error instanceof HttpException ? error.getStatus() : 500;
          logger.business(
            status < 500 ? 'warn' : 'error',
            `${fields.httpMethod} ${fields.route} ${status}`,
            {
              ...fields,
              status,
              durationMs: duration(),
              ...errorFields(error),
            },
          );
        },
      }),
    );
  }
}
