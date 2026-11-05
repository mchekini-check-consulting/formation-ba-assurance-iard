import { UpperCasePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Contrat, DemandeContrat } from '../../core/model/contrat';
import { FORMULES, LIBELLES_OPTIONS, Formule, OptionConseillee, formaterPrix } from '../../core/model/recommandation';
import { DevisService } from '../../core/service/devis.service';
import { ContratService } from '../../core/service/contrat.service';
import { ConnexionService } from '../../core/service/connexion.service';

type EtatPage = 'chargement' | 'affiche' | 'erreur-technique' | 'mandat-non-signe';

@Component({
  selector: 'iard-devis-contrat',
  imports: [RouterLink, UpperCasePipe],
  templateUrl: './devis-contrat.html',
  styleUrl: './devis-contrat.scss',
})
export class DevisContrat {
  private readonly devis = inject(DevisService);
  private readonly contrats = inject(ContratService);
  private readonly connexion = inject(ConnexionService);
  private readonly router = inject(Router);

  protected readonly etat = signal<EtatPage>('chargement');
  protected readonly contrat = signal<Contrat | null>(null);
  protected readonly email = this.connexion.emailClient;

  protected readonly mandatLu = signal(false);
  protected readonly codeSignature = signal('');
  protected readonly erreurSignature = signal<string | null>(null);
  protected readonly enCoursDeSignature = signal(false);

  protected readonly peutSigner = computed(
    () => this.mandatLu() && this.codeSignature().length === 6 && !this.enCoursDeSignature(),
  );

  protected readonly nomFormule = computed(() => {
    const c = this.contrat();
    if (!c) return '';
    return FORMULES[c.formule as Formule]?.nom ?? c.formule;
  });

  protected readonly plafondFormule = computed(() => {
    const c = this.contrat();
    if (!c) return '';
    return FORMULES[c.formule as Formule]?.plafond ?? '';
  });

  protected readonly libellesOptions = computed(() => {
    const c = this.contrat();
    if (!c || !c.options.length) return 'Aucune';
    return c.options.map((o) => LIBELLES_OPTIONS[o as OptionConseillee] ?? o).join(', ');
  });

  protected readonly formaterPrix = formaterPrix;

  constructor() {
    this.genererOuCharger();
  }

  protected saisirCode(valeur: string): void {
    this.codeSignature.set(valeur.replace(/\D/g, '').slice(0, 6));
    this.erreurSignature.set(null);
  }

  protected signerContrat(): void {
    const ref = this.devis.reference();
    if (!ref) return;
    this.enCoursDeSignature.set(true);
    this.erreurSignature.set(null);
    this.contrats.signer(ref, this.codeSignature()).subscribe((resultat) => {
      this.enCoursDeSignature.set(false);
      if (resultat.statut === 'signe') {
        this.contrat.set(resultat.contrat);
        this.codeSignature.set('');
      } else if (resultat.statut === 'code-incorrect') {
        this.erreurSignature.set('Code incorrect. Vérifiez le SMS reçu et réessayez.');
        this.codeSignature.set('');
      } else if (resultat.statut === 'session-expiree') {
        void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis/contrat' } });
      } else {
        this.erreurSignature.set('Une erreur est survenue. Veuillez réessayer.');
      }
    });
  }

  protected masquerTelephone(telephone: string | null | undefined): string {
    return masquerTelephone(telephone);
  }

  protected afficherIban(ibanDebut: string | null | undefined, ibanFin: string | null | undefined): string {
    return afficherIban(ibanDebut, ibanFin);
  }

  protected formaterDate(iso: string): string {
    return formaterDate(iso);
  }

  protected formaterDateEffet(iso: string): string {
    if (!iso) return '';
    const [annee, mois, jour] = iso.split('-');
    return `${jour}/${mois}/${annee} à 0 h`;
  }

  protected capitaliser(texte: string): string {
    return texte ? texte.toUpperCase() : '';
  }

  private genererOuCharger(): void {
    const demande = this.construireDemande();
    if (demande) {
      this.contrats.generer(demande).subscribe((resultat) => {
        if (resultat.statut === 'genere') {
          this.contrat.set(resultat.contrat);
          this.etat.set('affiche');
        } else if (resultat.statut === 'session-expiree') {
          void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis/contrat' } });
        } else if (resultat.statut === 'mandat-non-signe') {
          this.etat.set('mandat-non-signe');
        } else {
          this.etat.set('erreur-technique');
        }
      });
    } else {
      this.etat.set('erreur-technique');
    }
  }

  private construireDemande(): DemandeContrat | null {
    const reference = this.devis.reference();
    const logement = this.devis.logement();
    const franchise = this.devis.franchiseChoisie();
    const prixMensuel = this.devis.prixMensuel();

    if (!reference || !logement.adresse || !logement.codePostal || !franchise || prixMensuel === null) {
      return null;
    }

    return {
      referenceDevis: reference,
      adresseLogement: logement.adresse,
      codePostal: logement.codePostal,
      ville: logement.ville,
      typeLogement: logement.typeLogement ?? 'appartement',
      surface: Number(logement.surface),
      franchise: Number(franchise as string),
      cotisationMensuelle: prixMensuel,
      resiliationHamon: logement.antecedent === 'assure-plus-un-an',
    };
  }
}

function masquerTelephone(telephone: string | null | undefined): string {
  if (!telephone) return '';
  const t = telephone.replace(/\s/g, '');
  if (t.length !== 10) return telephone;
  return `${t.slice(0, 2)} •• •• •• ${t.slice(8)}`;
}

function afficherIban(ibanDebut: string | null | undefined, ibanFin: string | null | undefined): string {
  const debut = ibanDebut ?? '••••';
  const fin = ibanFin ?? '••••';
  return `${debut} •••• •••• •••• •••• •••• ${fin}`;
}

function formaterDate(iso: string): string {
  if (!iso) return '';
  const [annee, mois, jour] = iso.split('-');
  const moisNoms = [
    'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
    'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
  ];
  return `${Number(jour)} ${moisNoms[Number(mois) - 1]} ${annee}`;
}
