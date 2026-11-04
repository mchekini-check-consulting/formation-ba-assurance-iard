import { describe, expect, it } from 'vitest';
import { config } from './config';

describe('config', () => {
  it("déduit Keycloak et la route de santé du host de l'environnement", () => {
    expect(config('health.formation-ba-assurance-iard.qualif.check-consulting.net')).toEqual({
      keycloakUrl: 'https://keycloak.qualif.check-consulting.net',
      healthUrl:
        'https://gateway.formation-ba-assurance-iard.qualif.check-consulting.net/backend/actuator/health',
    });
  });

  it('utilise le Keycloak de dev et le proxy du serveur de dev en local', () => {
    expect(config('localhost')).toEqual({
      keycloakUrl: 'https://keycloak.dev.check-consulting.net',
      healthUrl: '/api/actuator/health',
    });
  });
});
