import { TitleCasePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Contrat, DemandeContrat } from '../../core/model/contrat';
import { FORMULES, LIBELLES_OPTIONS, Formule, OptionConseillee, formaterPrix } from '../../core/model/recommandation';
import { DevisService } from '../../core/service/devis.service';
import { ContratService } from '../../core/service/contrat.service';
import { ConnexionService } from '../../core/service/connexion.service';

type EtatPage = 'chargement' | 'affiche' | 'erreur-technique' | 'mandat-non-signe';
type EtatSignature = 'avant-code' | 'envoi-en-cours' | 'attente-code' | 'validation-en-cours' | 'essais-epuises';

@Component({
  selector: 'iard-devis-contrat',
  imports: [RouterLink, TitleCasePipe],
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
  protected readonly etatSignature = signal<EtatSignature>('avant-code');
  protected readonly emailMasque = signal<string | null>(null);
  protected readonly renonciationEtendue = signal(false);

  protected readonly peutSigner = computed(
    () => this.etatSignature() === 'attente-code' && this.codeSignature().length === 6,
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

  protected readonly residenceLabel = computed(() => {
    const r = this.devis.logement().residence;
    if (r === 'principale') return 'Résidence principale';
    if (r === 'secondaire') return 'Résidence secondaire';
    return '';
  });

  protected readonly statutLabel = computed(() => {
    switch (this.devis.logement().statut) {
      case 'locataire': return 'Locataire';
      case 'colocation': return 'Colocataire';
      case 'proprietaire-occupant': return 'Propriétaire occupant';
      case 'proprietaire-non-occupant': return 'Propriétaire non occupant';
      default: return '';
    }
  });

  protected readonly descriptionLogement = computed(() => {
    const l = this.devis.logement();
    const parties: string[] = [];
    if (l.nombrePieces) parties.push(`${l.nombrePieces} pièce${Number(l.nombrePieces) > 1 ? 's' : ''}`);
    if (l.etage) parties.push(`${l.etage}e étage`);
    return parties.join(' · ');
  });

  protected readonly formaterPrix = formaterPrix;

  constructor() {
    this.genererOuCharger();
  }

  protected saisirCode(valeur: string): void {
    this.codeSignature.set(valeur.replace(/\D/g, '').slice(0, 6));
    this.erreurSignature.set(null);
  }

  protected envoyerCode(): void {
    const ref = this.devis.reference();
    if (!ref) return;
    this.etatSignature.set('envoi-en-cours');
    this.contrats.envoyerCode(ref).subscribe((resultat) => {
      if (resultat.statut === 'envoye') {
        this.emailMasque.set(resultat.emailMasque);
        this.etatSignature.set('attente-code');
        this.codeSignature.set('');
        this.erreurSignature.set(null);
      } else if (resultat.statut === 'session-expiree') {
        void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis/contrat' } });
      } else {
        this.etatSignature.set('avant-code');
        this.erreurSignature.set("Impossible d'envoyer le code. Veuillez réessayer.");
      }
    });
  }

  protected renvoyerCode(): void {
    const ref = this.devis.reference();
    if (!ref) return;
    this.etatSignature.set('envoi-en-cours');
    this.erreurSignature.set(null);
    this.contrats.renvoyerCode(ref).subscribe((resultat) => {
      if (resultat.statut === 'envoye') {
        this.emailMasque.set(resultat.emailMasque);
        this.etatSignature.set('attente-code');
        this.codeSignature.set('');
        this.erreurSignature.set(null);
      } else if (resultat.statut === 'session-expiree') {
        void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis/contrat' } });
      } else {
        this.etatSignature.set('essais-epuises');
        this.erreurSignature.set("Impossible d'envoyer le code. Veuillez réessayer.");
      }
    });
  }

  protected signerContrat(): void {
    const ref = this.devis.reference();
    if (!ref) return;
    this.etatSignature.set('validation-en-cours');
    this.erreurSignature.set(null);
    this.contrats.signer(ref, this.codeSignature()).subscribe((resultat) => {
      if (resultat.statut === 'signe') {
        this.contrat.set(resultat.contrat);
        this.codeSignature.set('');
      } else if (resultat.statut === 'code-incorrect') {
        this.etatSignature.set('attente-code');
        const msg =
          resultat.essaisRestants !== null
            ? `Code incorrect. Il vous reste ${resultat.essaisRestants} essai(s).`
            : 'Code incorrect.';
        this.erreurSignature.set(msg);
        this.codeSignature.set('');
      } else if (resultat.statut === 'essais-epuises') {
        this.etatSignature.set('essais-epuises');
        this.erreurSignature.set(null);
        this.codeSignature.set('');
      } else if (resultat.statut === 'code-expire') {
        this.etatSignature.set('attente-code');
        this.erreurSignature.set('Votre code a expiré. Cliquez sur « Renvoyer le code ».');
        this.codeSignature.set('');
      } else if (resultat.statut === 'session-expiree') {
        void this.router.navigate(['/connexion'], { queryParams: { retour: '/devis/contrat' } });
      } else {
        this.etatSignature.set('attente-code');
        this.erreurSignature.set('Une erreur est survenue. Veuillez réessayer.');
      }
    });
  }

  protected abrevierCivilite(civilite: string | null | undefined): string {
    return abrevierCivilite(civilite);
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

function abrevierCivilite(civilite: string | null | undefined): string {
  if (civilite === 'madame') return 'Mme';
  if (civilite === 'monsieur') return 'M.';
  return '';
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
