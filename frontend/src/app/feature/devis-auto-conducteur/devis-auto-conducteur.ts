import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  AssureVehicule,
  ConducteurAuto,
  ConducteurPrincipal,
  KmAnnuels,
  ResiliationAuto,
  SinistresCount,
  kmAnnuelsVersEntier,
} from '../../core/model/devis-auto';
import { CONDITIONS_AUTO } from '../../core/config/conditions-auto';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { TarifAutoService, TarifAutoReponse } from '../../core/service/tarif-auto.service';
import { DevisAutoCreationService } from '../../core/service/devis-auto-creation.service';
import { DevisAutoLayout } from '../devis-auto-layout/devis-auto-layout';

function validerDateSaisie(str: string): string | null {
  if (!str.trim()) return 'Ce champ est obligatoire.';
  if (!/^\d{2}\/\d{2}\/\d{4}$/.test(str)) return 'Format attendu : JJ/MM/AAAA.';
  const [j, m, a] = str.split('/').map(Number);
  const d = new Date(a, m - 1, j);
  if (d.getDate() !== j || d.getMonth() !== m - 1 || d.getFullYear() !== a) {
    return 'Date invalide.';
  }
  if (d > new Date()) return 'La date ne peut pas Ãªtre dans le futur.';
  return null;
}

function extraireAnnee(dateDDMMYYYY: string): number | null {
  if (!/^\d{2}\/\d{2}\/\d{4}$/.test(dateDDMMYYYY)) return null;
  return parseInt(dateDDMMYYYY.slice(6, 10), 10);
}

function diffAnsDepuis(dateDDMMYYYY: string): number | null {
  if (!/^\d{2}\/\d{2}\/\d{4}$/.test(dateDDMMYYYY)) return null;
  const [j, m, a] = dateDDMMYYYY.split('/').map(Number);
  const d = new Date(a, m - 1, j);
  const ms = new Date().getTime() - d.getTime();
  return ms / (1000 * 60 * 60 * 24 * 365.25);
}

function formaterDateInput(e: Event): string {
  const raw = (e.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 8);
  let f = raw.slice(0, 2);
  if (raw.length > 2) f += '/' + raw.slice(2, 4);
  if (raw.length > 4) f += '/' + raw.slice(4, 8);
  (e.target as HTMLInputElement).value = f;
  return f;
}

@Component({
  selector: 'iard-devis-auto-conducteur',
  imports: [DevisAutoLayout, RouterLink],
  templateUrl: './devis-auto-conducteur.html',
  styleUrl: './devis-auto-conducteur.scss',
})
export class DevisAutoConducteur {
  protected readonly devisAutoService = inject(DevisAutoService);
  private readonly tarifAutoService = inject(TarifAutoService);
  private readonly creationService = inject(DevisAutoCreationService);
  private readonly router = inject(Router);

  private readonly saisieExistante = this.devisAutoService.conducteur();

  protected readonly conducteurPrincipal = signal<ConducteurPrincipal | null>(
    this.saisieExistante?.conducteurPrincipal ?? null,
  );
  protected readonly dateNaissance = signal<string>(this.saisieExistante?.dateNaissance ?? '');
  protected readonly datePermis = signal<string>(this.saisieExistante?.datePermis ?? '');
  protected readonly bonusMalus = signal<number | null>(this.saisieExistante?.bonusMalus ?? null);
  protected readonly anneesAssurance = signal<number | null>(
    this.saisieExistante?.anneesAssurance ?? null,
  );
  protected readonly conduiteAccompagnee = signal<boolean>(
    this.saisieExistante?.conduiteAccompagnee ?? false,
  );
  protected readonly nbSinistresResponsables = signal<SinistresCount | null>(
    this.saisieExistante?.nbSinistresResponsables ?? null,
  );
  protected readonly nbSinistresNonResponsables = signal<SinistresCount | null>(
    this.saisieExistante?.nbSinistresNonResponsables ?? null,
  );
  protected readonly resiliationAssureur = signal<ResiliationAuto | null>(
    this.saisieExistante?.resiliationAssureur ?? null,
  );
  protected readonly suspensionAnnulation = signal<boolean | null>(
    this.saisieExistante?.suspensionAnnulation ?? null,
  );
  protected readonly condamnationAlcool = signal<boolean | null>(
    this.saisieExistante?.condamnationAlcool ?? null,
  );
  protected readonly assureVehicule = signal<AssureVehicule | null>(
    this.saisieExistante?.assureVehicule ?? null,
  );
  protected readonly ancienAssureur = signal<string>(this.saisieExistante?.ancienAssureur ?? '');
  protected readonly ancienNumeroContrat = signal<string>(
    this.saisieExistante?.ancienNumeroContrat ?? '',
  );

  protected readonly tentative = signal(false);
  protected readonly enregistrement = signal(false);
  protected readonly erreurServeur = signal<string | null>(null);

  protected readonly calculEnCours = signal(false);
  protected readonly tarif = signal<TarifAutoReponse | null>(null);
  protected readonly bonusMalusMax = signal<number>(1.50);

  private readonly anneePermisCalculee = computed(() => extraireAnnee(this.datePermis()));
  private readonly agePermisAns = computed(() => diffAnsDepuis(this.datePermis()));

  protected readonly montrerConduiteAccompagnee = computed(() => {
    const age = this.agePermisAns();
    return age !== null && age < CONDITIONS_AUTO.conduiteAccompagneeSeuilAns;
  });

  protected readonly conducteurEstMineur = computed(() => {
    if (this.conducteurPrincipal() !== 'MOI_MEME') return false;
    if (!this.dateNaissance()) return false;
    const age = diffAnsDepuis(this.dateNaissance());
    return age !== null && age < 18;
  });

  protected readonly titulaireCarteGrise = computed(
    () => this.devisAutoService.achat()?.titulaireCarteGrise ?? null,
  );

  protected readonly messageInfoConducteur = computed(() => {
    const c = this.conducteurPrincipal();
    if (c === 'ENFANT' || c === 'CONJOINT' || c === 'AUTRE_PERSONNE_FOYER') {
      return "Le souscripteur reste juridiquement responsable du contrat. Le conducteur principal est la personne qui utilise le vÃ©hicule au quotidien.";
    }
    return null;
  });

  protected readonly conditionBloquante = computed((): string | null => {
    if (this.conducteurEstMineur()) {
      return "Un conducteur principal mineur ne permet pas la souscription en ligne.";
    }
    const bm = this.bonusMalus();
    if (bm !== null && bm > this.bonusMalusMax()) {
      return `Votre coefficient bonus-malus (${bm.toFixed(2).replace('.', ',')}) dÃ©passe le plafond acceptÃ©.`;
    }
    if (this.resiliationAssureur() === 'NON_PAIEMENT') {
      return "Une rÃ©siliation pour non-paiement empÃªche la souscription en ligne.";
    }
    if (this.suspensionAnnulation() === true) {
      return "Une suspension ou annulation de permis empÃªche la souscription en ligne.";
    }
    if (this.condamnationAlcool() === true) {
      return "Une condamnation pour alcool ou stupÃ©fiants empÃªche la souscription en ligne.";
    }
    const novice = (() => {
      const agePermis = this.agePermisAns();
      return (agePermis !== null && agePermis < 3) || this.anneesAssurance() === 0;
    })();
    if (novice && (this.nbSinistresResponsables() ?? 0) >= 1) {
      return "Un profil novice avec sinistre responsable ne peut pas souscrire en ligne.";
    }
    if (this.titulaireCarteGrise() === 'AUTRE_PERSONNE') {
      return "La carte grise au nom d'une autre personne empÃªche la souscription en ligne.";
    }
    return null;
  });

  // Erreurs de validation
  protected readonly erreurConducteurPrincipal = computed(() =>
    this.tentative() && !this.conducteurPrincipal() ? 'Indiquez le conducteur principal.' : null,
  );
  protected readonly erreurDateNaissance = computed(() =>
    this.tentative() && !this.conducteurEstMineur() ? validerDateSaisie(this.dateNaissance()) : null,
  );
  protected readonly erreurDatePermis = computed(() =>
    this.tentative() && !this.conducteurEstMineur() ? validerDateSaisie(this.datePermis()) : null,
  );
  protected readonly erreurBonusMalus = computed(() => {
    if (!this.tentative() || this.conducteurEstMineur()) return null;
    const bm = this.bonusMalus();
    if (bm === null) return 'Indiquez votre coefficient bonus-malus.';
    if (bm < 0.5 || bm > 3.5) return 'Le coefficient doit Ãªtre entre 0,50 et 3,50.';
    return null;
  });
  protected readonly erreurAnneesAssurance = computed(() =>
    this.tentative() && !this.conducteurEstMineur() && this.anneesAssurance() === null
      ? "Indiquez vos annÃ©es d'assurance continue."
      : null,
  );
  protected readonly erreurSinistresResponsables = computed(() =>
    this.tentative() && !this.conducteurEstMineur() && this.nbSinistresResponsables() === null
      ? 'Indiquez le nombre de sinistres responsables.'
      : null,
  );
  protected readonly erreurSinistresNonResponsables = computed(() =>
    this.tentative() && !this.conducteurEstMineur() && this.nbSinistresNonResponsables() === null
      ? 'Indiquez le nombre de sinistres non responsables.'
      : null,
  );
  protected readonly erreurResiliationAssureur = computed(() =>
    this.tentative() && !this.conducteurEstMineur() && !this.resiliationAssureur()
      ? 'RÃ©pondez Ã  cette question.'
      : null,
  );
  protected readonly erreurSuspensionAnnulation = computed(() =>
    this.tentative() && !this.conducteurEstMineur() && this.suspensionAnnulation() === null
      ? 'RÃ©pondez Ã  cette question.'
      : null,
  );
  protected readonly erreurCondamnationAlcool = computed(() =>
    this.tentative() && !this.conducteurEstMineur() && this.condamnationAlcool() === null
      ? 'RÃ©pondez Ã  cette question.'
      : null,
  );
  protected readonly erreurAssureVehicule = computed(() =>
    this.tentative() && !this.conducteurEstMineur() && !this.assureVehicule()
      ? 'RÃ©pondez Ã  cette question.'
      : null,
  );
  protected readonly erreurAncienAssureur = computed(() =>
    this.tentative() && this.assureVehicule() === 'OUI_PLUS_UN_AN' && !this.ancienAssureur().trim()
      ? "Indiquez votre assureur actuel."
      : null,
  );
  protected readonly erreurAncienNumeroContrat = computed(() =>
    this.tentative() &&
    this.assureVehicule() === 'OUI_PLUS_UN_AN' &&
    !this.ancienNumeroContrat().trim()
      ? "Indiquez votre numÃ©ro de contrat actuel."
      : null,
  );

  // Options des listes
  protected readonly conducteurs: { valeur: ConducteurPrincipal; libelle: string }[] = [
    { valeur: 'MOI_MEME', libelle: 'Moi' },
    { valeur: 'ENFANT', libelle: 'Mon enfant' },
    { valeur: 'CONJOINT', libelle: 'Mon conjoint' },
    { valeur: 'AUTRE_PERSONNE_FOYER', libelle: 'Une autre personne du foyer' },
  ];

  protected readonly tranchesAnnees: { valeur: number; libelle: string }[] = [
    { valeur: 0, libelle: "Aucune (1Ã¨re assurance)" },
    { valeur: 1, libelle: '1 an' },
    { valeur: 2, libelle: '2 ans' },
    { valeur: 3, libelle: '3 ans' },
  ];

  protected readonly optsResiliation: { valeur: ResiliationAuto; libelle: string }[] = [
    { valeur: 'AUCUNE', libelle: 'Non' },
    { valeur: 'NON_PAIEMENT', libelle: 'Oui â€“ Non-paiement' },
    { valeur: 'SINISTRALITE', libelle: 'Oui â€“ SinistralitÃ©' },
    { valeur: 'FAUSSE_DECLARATION', libelle: 'Oui â€“ Fausse dÃ©claration' },
    { valeur: 'AUTRE_MOTIF', libelle: 'Oui â€“ Autre motif' },
  ];

  protected readonly optsAssureVehicule: { valeur: AssureVehicule; libelle: string }[] = [
    { valeur: 'NON', libelle: 'Non' },
    { valeur: 'OUI_MOINS_UN_AN', libelle: 'Oui, moins d\'un an' },
    { valeur: 'OUI_PLUS_UN_AN', libelle: 'Oui, plus d\'un an' },
  ];

  private calculTimer: ReturnType<typeof setTimeout> | null = null;

  private readonly parametresTarif = computed(() => {
    const usage = this.devisAutoService.usage();
    const vehicule = this.devisAutoService.vehicule();
    if (!usage || !vehicule) return null;

    const anneePermis =
      this.anneePermisCalculee() ?? (new Date().getFullYear() - 15);
    const bm = this.bonusMalus() ?? 1.0;
    const bmValide = bm >= 0.5 && bm <= 3.5 ? bm : 1.0;

    return {
      refVehiculeId: vehicule.refVehiculeId > 0 ? vehicule.refVehiculeId : undefined,
      anneePermis,
      bonusMalus: bmValide,
      kmAnnuels: kmAnnuelsVersEntier(usage.kmAnnuels as KmAnnuels),
      usage: usage.usage,
      stationnement: usage.stationnement,
    };
  });

  constructor() {
    effect(() => {
      const params = this.parametresTarif();
      untracked(() => {
        if (this.calculTimer) clearTimeout(this.calculTimer);
        if (!params) {
          this.calculEnCours.set(false);
          return;
        }
        this.calculEnCours.set(true);
        this.calculTimer = setTimeout(() => {
          this.tarifAutoService.calculer(params).subscribe(rep => {
            this.calculEnCours.set(false);
            if (rep !== null) this.tarif.set(rep);
          });
        }, 300);
      });
    });
  }

  protected formaterPrix(prix: number): string {
    return prix.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected saisirDateNaissance(e: Event): void {
    this.dateNaissance.set(formaterDateInput(e));
  }

  protected saisirDatePermis(e: Event): void {
    this.datePermis.set(formaterDateInput(e));
  }

  protected saisirBonusMalus(e: Event): void {
    const raw = (e.target as HTMLInputElement).value.replace(',', '.');
    const v = parseFloat(raw);
    this.bonusMalus.set(isNaN(v) ? null : v);
  }

  protected choisirAnneesAssurance(e: Event): void {
    const v = (e.target as HTMLSelectElement).value;
    this.anneesAssurance.set(v === '' ? null : parseInt(v, 10));
  }

  protected choisirSinistresResponsables(e: Event): void {
    const v = (e.target as HTMLSelectElement).value;
    this.nbSinistresResponsables.set(v === '' ? null : (parseInt(v, 10) as SinistresCount));
  }

  protected choisirSinistresNonResponsables(e: Event): void {
    const v = (e.target as HTMLSelectElement).value;
    this.nbSinistresNonResponsables.set(v === '' ? null : (parseInt(v, 10) as SinistresCount));
  }

  protected choisirResiliation(e: Event): void {
    const v = (e.target as HTMLSelectElement).value as ResiliationAuto;
    this.resiliationAssureur.set(v === ('' as ResiliationAuto) ? null : v);
  }

  protected choisirSuspension(e: Event): void {
    const v = (e.target as HTMLSelectElement).value;
    this.suspensionAnnulation.set(v === '' ? null : v === 'OUI');
  }

  protected choisirCondamnation(e: Event): void {
    const v = (e.target as HTMLSelectElement).value;
    this.condamnationAlcool.set(v === '' ? null : v === 'OUI');
  }

  protected choisirAssureVehicule(e: Event): void {
    const v = (e.target as HTMLSelectElement).value as AssureVehicule;
    this.assureVehicule.set(v === ('' as AssureVehicule) ? null : v);
  }

  protected continuer(e: Event): void {
    e.preventDefault();
    this.tentative.set(true);
    this.erreurServeur.set(null);

    if (this.conducteurEstMineur()) return;
    if (this.conditionBloquante()) return;

    const bmOk = (() => {
      const bm = this.bonusMalus();
      return bm !== null && bm >= 0.5 && bm <= 3.5;
    })();

    const hamonOk =
      this.assureVehicule() !== 'OUI_PLUS_UN_AN' ||
      (!!this.ancienAssureur().trim() && !!this.ancienNumeroContrat().trim());

    const ok =
      !!this.conducteurPrincipal() &&
      !validerDateSaisie(this.dateNaissance()) &&
      !validerDateSaisie(this.datePermis()) &&
      bmOk &&
      this.anneesAssurance() !== null &&
      this.nbSinistresResponsables() !== null &&
      this.nbSinistresNonResponsables() !== null &&
      !!this.resiliationAssureur() &&
      this.suspensionAnnulation() !== null &&
      this.condamnationAlcool() !== null &&
      !!this.assureVehicule() &&
      hamonOk;

    if (!ok) {
      setTimeout(() => {
        const el = document.querySelector<HTMLElement>('[role="alert"]');
        el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 0);
      return;
    }

    const conducteur: ConducteurAuto = {
      conducteurPrincipal: this.conducteurPrincipal()!,
      dateNaissance: this.dateNaissance(),
      datePermis: this.datePermis(),
      bonusMalus: this.bonusMalus()!,
      anneesAssurance: this.anneesAssurance()!,
      conduiteAccompagnee: this.montrerConduiteAccompagnee() ? this.conduiteAccompagnee() : false,
      nbSinistresResponsables: this.nbSinistresResponsables()!,
      nbSinistresNonResponsables: this.nbSinistresNonResponsables()!,
      resiliationAssureur: this.resiliationAssureur()!,
      suspensionAnnulation: this.suspensionAnnulation()!,
      condamnationAlcool: this.condamnationAlcool()!,
      assureVehicule: this.assureVehicule()!,
      ancienAssureur: this.ancienAssureur(),
      ancienNumeroContrat: this.ancienNumeroContrat(),
    };

    this.devisAutoService.enregistrerConducteur(conducteur);

    // Envoyer au backend
    const vehicule = this.devisAutoService.vehicule()!;
    const achat = this.devisAutoService.achat()!;
    const usage = this.devisAutoService.usage()!;

    this.enregistrement.set(true);
    this.creationService
      .creer({
        refVehiculeId: vehicule.refVehiculeId > 0 ? vehicule.refVehiculeId : undefined,
        immatriculation: achat.immatriculation ?? undefined,
        nonImmatricule: achat.nonImmatricule,
        titulaireCarteGrise: achat.titulaireCarteGrise,
        dateAcquisition: achat.dateAcquisition,
        modeAchat: achat.modeAchat,
        usage: usage.usage,
        stationnement: usage.stationnement,
        kmAnnuels: kmAnnuelsVersEntier(usage.kmAnnuels as KmAnnuels),
        codePostal: usage.codePostal,
        conducteurPrincipal: conducteur.conducteurPrincipal,
        dateNaissance: conducteur.dateNaissance,
        datePermis: conducteur.datePermis,
        bonusMalus: conducteur.bonusMalus,
        anneesAssurance: conducteur.anneesAssurance,
        conduiteAccompagnee: conducteur.conduiteAccompagnee,
        nbSinistresResponsables: conducteur.nbSinistresResponsables,
        nbSinistresNonResponsables: conducteur.nbSinistresNonResponsables,
        resiliationAssureur: conducteur.resiliationAssureur,
        suspensionAnnulation: conducteur.suspensionAnnulation,
        condamnationAlcool: conducteur.condamnationAlcool,
        assureVehicule: conducteur.assureVehicule,
        ancienAssureur: conducteur.ancienAssureur || undefined,
        ancienNumeroContrat: conducteur.ancienNumeroContrat || undefined,
      })
      .subscribe({
        next: (rep) => {
          this.devisAutoService.enregistrerDevisAutoId(rep.devisAutoId);
          this.devisAutoService.enregistrerTarifEtape1(this.tarif());
          this.enregistrement.set(false);
          this.router.navigate(['/devis-auto/besoins']);
        },
        error: (err) => {
          this.enregistrement.set(false);
          if (err?.status === 422 && err?.error?.motif) {
            this.erreurServeur.set("Votre profil ne permet pas la souscription en ligne.");
          } else {
            this.erreurServeur.set("Une erreur est survenue. Veuillez rÃ©essayer.");
          }
        },
      });
  }
}


