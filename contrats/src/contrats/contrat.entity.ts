import {
  Column,
  CreateDateColumn,
  Entity,
  PrimaryGeneratedColumn,
} from 'typeorm';

// Contrat exposé par /contrats (table créée par la migration Contrat)
@Entity('contrat')
export class Contrat {
  @PrimaryGeneratedColumn({ type: 'bigint' })
  id: string;

  @Column({ type: 'varchar', length: 50 })
  numero: string;

  @Column({ type: 'varchar', length: 200 })
  assure: string;

  @CreateDateColumn({ name: 'cree_le', type: 'timestamptz' })
  creeLe: Date;
}
