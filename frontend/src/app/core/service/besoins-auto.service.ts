import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { apiBase } from '../config/api';
import { BesoinsAuto, FormuleConseillee, PrioriteAuto, RecommandationAuto } from '../model/devis-auto';

export interface BesoinsAutoRequete {
  valeurVehicule: number;
  vehiculeIndispensable: boolean;
  assistance0km: boolean;
  protectionConducteur: boolean;
  brisGlace: boolean;
  accompagnementLitige: boolean;
  priorite: PrioriteAuto;
}

export interface BesoinsAutoReponse {
  formuleConseillee: FormuleConseillee;
  raisons: string[];
  prixMensuel: number | null;
}

@Injectable({ providedIn: 'root' })
export class BesoinsAutoService {
  private readonly http = inject(HttpClient);

  enregistrer(devisAutoId: number, requete: BesoinsAutoRequete): Observable<BesoinsAutoReponse> {
    const url = `${apiBase('assurance-iard-api')}/v1/devis/auto/${devisAutoId}/besoins`;
    return this.http.post<BesoinsAutoReponse>(url, requete);
  }
}

export function besoinsVersRequete(b: BesoinsAuto): BesoinsAutoRequete {
  return {
    valeurVehicule: b.valeurVehicule,
    vehiculeIndispensable: b.vehiculeIndispensable,
    assistance0km: b.assistance0km,
    protectionConducteur: b.protectionConducteur,
    brisGlace: b.brisGlace,
    accompagnementLitige: b.accompagnementLitige,
    priorite: b.priorite,
  };
}

export function reponseVersRecommandation(rep: BesoinsAutoReponse): RecommandationAuto {
  return {
    formuleConseillee: rep.formuleConseillee,
    raisons: rep.raisons,
    prixMensuel: rep.prixMensuel,
  };
}
