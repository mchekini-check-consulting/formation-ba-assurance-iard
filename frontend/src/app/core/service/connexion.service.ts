import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, tap } from 'rxjs';
import { apiBase } from '../config/api';
import { Droit, PROFILS_INTERNES, Rubrique, SessionCourante } from '../model/droits';
import {
  MotifNouveauCode,
  ResultatCode,
  ResultatConnexion,
  ResultatRenvoi,
  SessionOuverte,
  VerificationEnCours,
} from '../model/connexion';

const CLE_JETON = 'assureasy.session';

function statutDe(erreur: unknown): number | null {
  return erreur instanceof HttpErrorResponse ? erreur.status : null;
}

@Injectable({ providedIn: 'root' })
export class ConnexionService {
  private readonly http = inject(HttpClient);
  private readonly url = apiBase('assurance-iard-api');

  /** Étape « double authentification » en cours, gardée en mémoire : un rechargement renvoie à la connexion. */
  readonly verification = signal<VerificationEnCours | null>(null);
  readonly emailClient = signal<string | null>(null);
  readonly profilClient = signal<string | null>(null);
  /** US-04 : droits du profil (« RUBRIQUE:DROIT »), relus auprès du serveur à chaque changement de page. */
  readonly droitsClient = signal<readonly string[]>([]);

  /** Jeton de session, oublié à la fermeture de l'onglet (RG1). */
  jeton(): string | null {
    return sessionStorage.getItem(CLE_JETON);
  }

  connecter(email: string, motDePasse: string): Observable<ResultatConnexion> {
    return this.http.post<VerificationEnCours>(`${this.url}/connexion`, { email: email.trim(), motDePasse }).pipe(
      tap((verification) => this.verification.set(verification)),
      map((): ResultatConnexion => ({ statut: 'code-envoye' })),
      catchError((erreur: unknown) => of(this.resultatConnexionRefusee(erreur))),
    );
  }

  verifierCode(code: string): Observable<ResultatCode> {
    const verification = this.verification()?.verification;
    return this.http.post<SessionOuverte>(`${this.url}/connexion/verification`, { verification, code }).pipe(
      tap((session) => {
        sessionStorage.setItem(CLE_JETON, session.jeton);
        this.emailClient.set(session.email);
        this.profilClient.set(session.profil);
        this.verification.set(null);
      }),
      map((): ResultatCode => ({ statut: 'connecte' })),
      catchError((erreur: unknown) => of(this.resultatCodeRefuse(erreur))),
    );
  }

  renvoyerCode(): Observable<ResultatRenvoi> {
    const verification = this.verification()?.verification;
    return this.http.post<VerificationEnCours>(`${this.url}/connexion/renvoi-code`, { verification }).pipe(
      map((): ResultatRenvoi => 'code-renvoye'),
      catchError((erreur: unknown) =>
        of<ResultatRenvoi>(statutDe(erreur) === 404 ? 'verification-perdue' : 'erreur-technique'),
      ),
    );
  }

  /** Vérifie auprès du backend que la session est toujours valable (AC6). */
  sessionValide(): Observable<boolean> {
    if (!this.jeton()) {
      return of(false);
    }
    return this.http.get<SessionCourante>(`${this.url}/session`).pipe(
      tap((session) => {
        this.emailClient.set(session.email);
        this.profilClient.set(session.profil);
        this.droitsClient.set(session.droits ?? []);
      }),
      map(() => true),
      catchError((erreur: unknown) => {
        if (statutDe(erreur) === 401) {
          this.oublierSession();
        }
        return of(false);
      }),
    );
  }

  deconnecter(): Observable<void> {
    return this.http.delete<void>(`${this.url}/session`).pipe(
      catchError(() => of(undefined)),
      finalize(() => this.oublierSession()),
    );
  }

  private oublierSession(): void {
    sessionStorage.removeItem(CLE_JETON);
    this.emailClient.set(null);
    this.profilClient.set(null);
    this.droitsClient.set([]);
  }

  /** US-04, AC3 : le profil a-t-il ce droit (tableau de l'US-02) ? */
  aLeDroit(rubrique: Rubrique, droit: Droit): boolean {
    return this.droitsClient().includes(`${rubrique}:${droit}`);
  }

  /** US-04 : profils du back-office (Gestionnaire, Administrateur). */
  estInterne(): boolean {
    return PROFILS_INTERNES.includes(this.profilClient() ?? '');
  }

  private resultatConnexionRefusee(erreur: unknown): ResultatConnexion {
    const statut = statutDe(erreur);
    if (statut === 401) {
      return { statut: 'identifiants-incorrects' };
    }
    if (statut === 423) {
      return { statut: 'compte-bloque', bloqueJusquA: (erreur as HttpErrorResponse).error.bloqueJusquA };
    }
    return { statut: 'erreur-technique' };
  }

  private resultatCodeRefuse(erreur: unknown): ResultatCode {
    const statut = statutDe(erreur);
    if (statut === 401) {
      return { statut: 'code-incorrect', essaisRestants: (erreur as HttpErrorResponse).error.essaisRestants };
    }
    if (statut === 409) {
      return { statut: 'nouveau-code-requis', motif: (erreur as HttpErrorResponse).error.motif as MotifNouveauCode };
    }
    if (statut === 404) {
      return { statut: 'verification-perdue' };
    }
    return { statut: 'erreur-technique' };
  }
}
