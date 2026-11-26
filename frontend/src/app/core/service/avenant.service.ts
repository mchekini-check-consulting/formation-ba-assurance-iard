import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { apiBase } from '../config/api';
import { Avenant, AvenantAccepte, GarantiesContrat, ProjetAvenant } from '../model/avenant';

export const MESSAGE_NON_MODIFIABLE =
  'Les garanties de ce contrat ne peuvent pas être modifiées dans sa situation actuelle.';
export const MESSAGE_AUCUN_AVENANT = "Aucun avenant n'attend votre réponse sur ce contrat.";
export const MESSAGE_ERREUR_TECHNIQUE = "L'opération n'a pas pu aboutir. Réessayez dans quelques instants.";
export const MESSAGE_TARIFS_INDISPONIBLES =
  'Les prix des garanties sont momentanément indisponibles. Réessayez dans quelques instants.';

/** Une demande refusée, avec un message compréhensible (jamais de donnée technique). */
export type Refus =
  /** 422 : contrat qui n'est pas actif (AC6). */
  | { statut: 'non-modifiable'; message: string }
  /** 400 : garantie obligatoire, aucun changement, case non cochée. */
  | { statut: 'invalide'; message: string }
  /** 404 : contrat inconnu ou aucun avenant en attente. */
  | { statut: 'introuvable'; message: string }
  | { statut: 'erreur-technique'; message: string };

export type ResultatGaranties = { statut: 'obtenu'; garanties: GarantiesContrat } | Refus;
export type ResultatPreparation = { statut: 'prepare'; projet: ProjetAvenant } | Refus;
export type ResultatAcceptation = { statut: 'accepte'; acceptation: AvenantAccepte } | Refus;
export type ResultatAbandon = { statut: 'abandonne'; avenant: Avenant } | Refus;

/**
 * Recette US-23 : réponses 400 techniques à ne jamais montrer au client (refus du moteur tarifaire avec la liste des
 * champs, validation du corps de la requête par Spring).
 */
function estTechnique(corps: { detail?: string; erreurs?: unknown } | null): boolean {
  const detail = corps?.detail ?? '';
  return !!corps?.erreurs || /champs invalides|invalid request/i.test(detail);
}

/** Message du serveur s'il en donne un (déjà en français, sans donnée technique), sinon un message par défaut. */
function refus(erreur: HttpErrorResponse): Refus {
  const corps = erreur.error as { detail?: string; erreurs?: unknown } | null;
  const detail = corps?.detail;
  switch (erreur.status) {
    case 422:
      return { statut: 'non-modifiable', message: detail ?? MESSAGE_NON_MODIFIABLE };
    case 400:
      return { statut: 'invalide', message: !detail || estTechnique(corps) ? MESSAGE_ERREUR_TECHNIQUE : detail };
    case 404:
      return { statut: 'introuvable', message: detail ?? MESSAGE_AUCUN_AVENANT };
    case 503:
      return { statut: 'erreur-technique', message: MESSAGE_TARIFS_INDISPONIBLES };
    default:
      return { statut: 'erreur-technique', message: MESSAGE_ERREUR_TECHNIQUE };
  }
}

/**
 * US-23 : modifier ses garanties et accepter l'avenant, via le gateway (backend assurance-iard-api). Un refus d'accès
 * (403) est affiché par la page « Accès non autorisé » (intercepteur d'US-04).
 */
@Injectable({ providedIn: 'root' })
export class AvenantService {
  private readonly http = inject(HttpClient);
  private readonly base = `${apiBase('assurance-iard-api')}/contrats`;

  private url(numero: string, chemin: string): string {
    return `${this.base}/${encodeURIComponent(numero)}/${chemin}`;
  }

  /** AC1, AC2, AC5 : garanties actuelles et ajoutables, avec leur prix ; avenant en attente. */
  garanties(numero: string): Observable<ResultatGaranties> {
    return this.http.get<GarantiesContrat>(this.url(numero, 'garanties')).pipe(
      map((garanties): ResultatGaranties => ({ statut: 'obtenu', garanties })),
      catchError((erreur: HttpErrorResponse) => of(refus(erreur))),
    );
  }

  /** AC3 : prépare l'avenant (garanties voulues après l'avenant). */
  preparer(numero: string, garanties: string[]): Observable<ResultatPreparation> {
    return this.http.post<ProjetAvenant>(this.url(numero, 'avenants'), { garanties }).pipe(
      map((projet): ResultatPreparation => ({ statut: 'prepare', projet })),
      catchError((erreur: HttpErrorResponse) => of(refus(erreur))),
    );
  }

  /** AC4 : accepte l'avenant en attente (case cochée). */
  accepter(numero: string): Observable<ResultatAcceptation> {
    return this.http.post<AvenantAccepte>(this.url(numero, 'avenants/en-attente/acceptation'), { accepte: true }).pipe(
      map((acceptation): ResultatAcceptation => ({ statut: 'accepte', acceptation })),
      catchError((erreur: HttpErrorResponse) => of(refus(erreur))),
    );
  }

  /** AC5 : abandonne l'avenant en attente. */
  abandonner(numero: string): Observable<ResultatAbandon> {
    return this.http.post<Avenant>(this.url(numero, 'avenants/en-attente/abandon'), null).pipe(
      map((avenant): ResultatAbandon => ({ statut: 'abandonne', avenant })),
      catchError((erreur: HttpErrorResponse) => of(refus(erreur))),
    );
  }

  /** AC3 : adresse de l'IPID d'une formule auto (document commun, public). */
  adresseIpid(formule: string): string {
    return `${apiBase('assurance-iard-api')}/documents-contrat/ipid/${encodeURIComponent(formule)}`;
  }
}
