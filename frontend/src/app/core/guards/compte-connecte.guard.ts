import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { CompteService } from '../service/compte.service';

export const compteConnecteGuard: CanActivateFn = () =>
  inject(CompteService).emailConnecte() ? true : inject(Router).parseUrl('/inscription');
