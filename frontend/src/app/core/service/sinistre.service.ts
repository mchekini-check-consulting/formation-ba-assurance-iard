import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import {
  ContexteDeclaration,
  DeclarationSinistre,
  MESSAGE_TAILLE,
  ReponsesSinistre,
  SinistreEnvoye,
  SinistreResume,
  TypesSinistre,
} from '../model/sinistre';

export const MESSAGE_ERREUR_TECHNIQUE = "L'opération n'a pas pu aboutir. Réessayez dans quelques instants.";
export const MESSAGE_INTROUVABLE = 'Cette déclaration est introuvable. Un brouillon est conservé 30 jours.';
export const MESSAGE_NON_DECLARABLE =
  "Ce contrat ne permet pas de déclarer un sinistre en ligne dans sa situation actuelle. Appelez notre service d'assistance pour être accompagné.";
export const MESSAGE_STOCKAGE =
  "Votre pièce n'a pas pu être enregistrée. Réessayez dans quelques instants ou envoyez-la plus tard : votre déclaration est conservée et peut être envoyée sans cette pièce.";

/** Une demande refusée, avec un message compréhensible (jamais de donnée technique). */
export interface Refus {
  statut: 'refuse';
  /** Code HTTP (0 si le serveur ne répond pas). */
  code: number;
  message: string;
}

export type Resultat<T> = { statut: 'ok'; valeur: T } | Refus;

/** Réponses techniques à ne jamais montrer (validation Spring, liste de champs). */
function estTechnique(corps: { detail?: string; erreurs?: unknown } | null): boolean {
  return !!corps?.erreurs || /champs invalides|invalid request|failed to/i.test(corps?.detail ?? '');
}

const PAR_DEFAUT: Record<number, string> = {
  404: MESSAGE_INTROUVABLE,
  413: MESSAGE_TAILLE,
  422: MESSAGE_NON_DECLARABLE,
  503: MESSAGE_STOCKAGE,
};

/** Message du serveur s'il en donne un (déjà en français), sinon un message par défaut. */
export function refus(erreur: HttpErrorResponse): Refus {
  const corps = erreur.error as { detail?: string; erreurs?: unknown } | null;
  const detail = corps?.detail;
  const parDefaut = PAR_DEFAUT[erreur.status] ?? MESSAGE_ERREUR_TECHNIQUE;
  const lisible = !!detail && !estTechnique(corps) && erreur.status >= 400 && erreur.status < 500 && erreur.status !== 413;
  return { statut: 'refuse', code: erreur.status, message: lisible ? detail : parDefaut };
}

function resultat<T>(appel: Observable<T>): Observable<Resultat<T>> {
  return appel.pipe(
    map((valeur): Resultat<T> => ({ statut: 'ok', valeur })),
    catchError((erreur: HttpErrorResponse) => of(refus(erreur))),
  );
}

/**
 * US-24 : déclaration de sinistre, via le gateway (backend assurance-iard-api). Un refus d'accès (403) est affiché par
 * la page « Accès non autorisé » (intercepteur d'US-04).
 */
@Injectable({ providedIn: 'root' })
export class SinistreService {
  private readonly http = inject(HttpClient);
  private readonly base = apiBase('assurance-iard-api');

  private url(id: number, ...suite: (string | number)[]): string {
    return [`${this.base}/sinistres/${id}`, ...suite.map((s) => encodeURIComponent(String(s)))].join('/');
  }

  /** AC1, AC10 : contrats ouverts à la déclaration et numéro d'assistance. */
  contexte(): Observable<Resultat<ContexteDeclaration>> {
    return resultat(this.http.get<ContexteDeclaration>(`${this.base}/sinistres/declaration`));
  }

  /** AC2 : types de sinistres du produit du contrat. */
  types(numeroContrat: string): Observable<Resultat<TypesSinistre>> {
    return resultat(
      this.http.get<TypesSinistre>(`${this.base}/contrats/${encodeURIComponent(numeroContrat)}/types-sinistre`),
    );
  }

  /** AC8 : « Mes sinistres ». */
  lister(): Observable<Resultat<SinistreResume[]>> {
    return resultat(this.http.get<SinistreResume[]>(`${this.base}/sinistres`));
  }

  /** AC1, AC2 : crée le brouillon. */
  creer(numeroContrat: string, type: string): Observable<Resultat<DeclarationSinistre>> {
    return resultat(this.http.post<DeclarationSinistre>(`${this.base}/sinistres`, { numeroContrat, type }));
  }

  lire(id: number): Observable<Resultat<DeclarationSinistre>> {
    return resultat(this.http.get<DeclarationSinistre>(this.url(id)));
  }

  /** AC4 à AC13 : enregistre les réponses du brouillon (toutes, à chaque fois). */
  enregistrer(id: number, reponses: ReponsesSinistre): Observable<Resultat<DeclarationSinistre>> {
    return resultat(this.http.put<DeclarationSinistre>(this.url(id), reponses));
  }

  /** AC9 : envoie la déclaration. */
  envoyer(id: number, declarationHonneur: boolean): Observable<Resultat<SinistreEnvoye>> {
    return resultat(this.http.post<SinistreEnvoye>(this.url(id, 'envoi'), { declarationHonneur }));
  }

  /** AC14 à AC16 : dépose un justificatif. */
  deposer(id: number, typePiece: string, fichier: File): Observable<Resultat<DeclarationSinistre>> {
    const formulaire = new FormData();
    formulaire.append('typePiece', typePiece);
    formulaire.append('fichier', fichier, fichier.name);
    return resultat(this.http.post<DeclarationSinistre>(this.url(id, 'pieces'), formulaire));
  }

  supprimer(id: number, pieceId: number): Observable<Resultat<DeclarationSinistre>> {
    return resultat(this.http.delete<DeclarationSinistre>(this.url(id, 'pieces', pieceId)));
  }

  /** Le fichier d'un justificatif déposé. */
  telecharger(id: number, pieceId: number): Observable<Resultat<Blob>> {
    return resultat(this.http.get(this.url(id, 'pieces', pieceId), { responseType: 'blob' }));
  }
}
