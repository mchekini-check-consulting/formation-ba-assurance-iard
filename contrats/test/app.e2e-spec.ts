import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import {
  PostgreSqlContainer,
  StartedPostgreSqlContainer,
} from '@testcontainers/postgresql';
import { AppModule } from './../src/app.module.js';

// Application complète, connectée à un PostgreSQL 16 jetable (migrations comprises).
// make test-integration ; Docker requis.
describe('AppController (e2e)', () => {
  let app: INestApplication<App>;
  let postgres: StartedPostgreSqlContainer;

  beforeAll(async () => {
    postgres = await new PostgreSqlContainer('postgres:16').start();
    process.env.DATABASE_URL = postgres.getConnectionUri();
  });

  afterAll(async () => {
    await postgres.stop();
  });

  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  it('/ (GET)', () => {
    return request(app.getHttpServer())
      .get('/')
      .expect(200)
      .expect('Hello World!');
  });

  it('/contrats (POST puis GET)', async () => {
    await request(app.getHttpServer())
      .post('/contrats')
      .send({ numero: 'CTR-001', assure: 'Jean Dupont' })
      .expect(201);
    const reponse = await request(app.getHttpServer())
      .get('/contrats')
      .expect(200);
    expect(
      reponse.body.map((contrat: { numero: string }) => contrat.numero),
    ).toContain('CTR-001');
  });

  it('/contrats (POST sans assuré) : 400', () => {
    return request(app.getHttpServer())
      .post('/contrats')
      .send({ numero: 'CTR-002' })
      .expect(400);
  });

  afterEach(async () => {
    await app.close();
  });
});
