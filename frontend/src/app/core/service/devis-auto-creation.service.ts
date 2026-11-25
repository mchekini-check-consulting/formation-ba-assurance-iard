import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiBase } from '../config/api';

export interface DevisAutoCreationRequete {
  refVehiculeId?: number;
  immatriculation?: string;
  nonImmatricule: boolean;
  titulaireCarteGrise?: string;
  dateAcquisition?: string;
  modeAchat?: string;
  usage: string;
  stationnement: string;
  kmAnnuels: number;
  codePostal: string;
  conducteurPrincipal: string;
  dateNaissance: string;
  datePermis: string;
  bonusMalus: number;
  anneesAssurance: number;
  conduiteAccompagnee: boolean;
  nbSinistresResponsables: number;
  nbSinistresNonResponsables: number;
  resiliationAssureur: string;
  suspensionAnnulation: boolean;
  condamnationAlcool: boolean;
  assureVehicule: string;
  ancienAssureur?: string;
  ancienNumeroContrat?: string;
}

export interface DevisAutoCreationReponse {
  devisAutoId: number;
  statutDevis: string;
  eligibleEnLigne: boolean;
}

@Injectable({ providedIn: 'root' })
export class DevisAutoCreationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${apiBase('assurance-iard-api')}/v1/devis/auto`;

  creer(requete: DevisAutoCreationRequete): Observable<DevisAutoCreationReponse> {
    return this.http.post<DevisAutoCreationReponse>(this.base, requete);
  }
}
