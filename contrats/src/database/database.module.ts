import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Init1761004800000 } from '../migrations/1761004800000-init.js';
import { Contrat1792800000000 } from '../migrations/1792800000000-contrat.js';

// Base PostgreSQL : DATABASE_URL, injectée par la plateforme (helm.yaml : database).
// Sans DATABASE_URL (poste de dev) : la base de compose.yaml.
// Lue au démarrage (forRootAsync), pas à l'import : les tests e2e la fixent avant.
@Module({
  imports: [
    TypeOrmModule.forRootAsync({
      useFactory: () => ({
        type: 'postgres' as const,
        url:
          process.env.DATABASE_URL ??
          'postgresql://app:app@localhost:5432/contrats',
        autoLoadEntities: true,
        // Le schéma vient uniquement des migrations, appliquées au démarrage
        synchronize: false,
        migrations: [Init1761004800000, Contrat1792800000000],
        migrationsRun: true,
      }),
    }),
  ],
})
export class DatabaseModule {}
