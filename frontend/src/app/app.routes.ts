import { Routes } from '@angular/router';
import { Landing } from './feature/landing/landing';

export const routes: Routes = [
  { path: '', component: Landing, title: "Assur'Easy – Assurance habitation en ligne" },
  { path: '**', redirectTo: '' },
];
