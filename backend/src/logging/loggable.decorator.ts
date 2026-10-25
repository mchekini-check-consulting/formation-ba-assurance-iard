import { HttpException } from '@nestjs/common';
import { errorFields, logger } from './json-logger.js';

// Décorateur de classe pour les services métier (layer service) : chaque méthode
// publique produit un log (classe, méthode, durée), synchrone ou asynchrone.
// Les arguments et les résultats ne sont jamais loggés (données personnelles).
// Exception HTTP (règle métier, ex. BadRequestException) : warn ; autre : error.
//
//   @Loggable()
//   @Injectable()
//   export class ItemsService { ... }
export function Loggable(): ClassDecorator {
  return (target) => {
    const prototype = target.prototype as Record<string, unknown>;
    for (const name of Object.getOwnPropertyNames(prototype)) {
      const descriptor = Object.getOwnPropertyDescriptor(prototype, name);
      if (name === 'constructor' || typeof descriptor?.value !== 'function') {
        continue;
      }
      descriptor.value = wrap(target.name, name, descriptor.value);
      Object.defineProperty(prototype, name, descriptor);
    }
  };
}

type Method = (...args: unknown[]) => unknown;

function wrap(className: string, method: string, original: Method): Method {
  const fields = { layer: 'service' as const, class: className, method };
  return function (this: unknown, ...args: unknown[]) {
    const start = performance.now();
    const duration = () => Math.round(performance.now() - start);
    const success = () =>
      logger.business('info', `${className}.${method} ok`, {
        ...fields,
        durationMs: duration(),
      });
    const failure = (error: unknown) =>
      logger.business(
        error instanceof HttpException && error.getStatus() < 500
          ? 'warn'
          : 'error',
        `${className}.${method} en échec`,
        { ...fields, durationMs: duration(), ...errorFields(error) },
      );

    let result: unknown;
    try {
      result = original.apply(this, args);
    } catch (error) {
      failure(error);
      throw error;
    }
    if (result instanceof Promise) {
      return result.then(
        (value: unknown) => {
          success();
          return value;
        },
        (error: unknown) => {
          failure(error);
          throw error;
        },
      );
    }
    success();
    return result;
  };
}
