import { MigrationInterface, QueryRunner } from 'typeorm';

// Migrations TypeORM, appliquées au démarrage dans l'ordre de leur horodatage ;
// une migration appliquée ne se modifie plus. Chaque nouvelle migration est
// ajoutée à la liste de database.module.ts, de façon compatible avec la version
// précédente (2 réplicas en preprod/prod pendant un déploiement).
// Init : schéma initial vide.
export class Init1761004800000 implements MigrationInterface {
  public async up(_queryRunner: QueryRunner): Promise<void> {}

  public async down(_queryRunner: QueryRunner): Promise<void> {}
}
