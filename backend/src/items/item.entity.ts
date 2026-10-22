import {
  Column,
  CreateDateColumn,
  Entity,
  PrimaryGeneratedColumn,
} from 'typeorm';

// Ressource générique exposée par /items (table créée par la migration Item),
// modèle à renommer par l'équipe
@Entity('item')
export class Item {
  @PrimaryGeneratedColumn({ type: 'bigint' })
  id: string;

  @Column({ type: 'varchar', length: 200 })
  nom: string;

  @Column({ type: 'varchar', length: 1000, nullable: true })
  description: string | null;

  @CreateDateColumn({ name: 'cree_le', type: 'timestamptz' })
  creeLe: Date;
}
