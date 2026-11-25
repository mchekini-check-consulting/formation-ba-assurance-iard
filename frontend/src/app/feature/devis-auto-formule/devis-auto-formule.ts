import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormuleMoteur, OptionAuto, PrixFormuleAuto, PrixOptionAuto } from '../../core/model/devis-auto';
import { apiBase } from '../../core/config/api';
import { DevisAutoService } from '../../core/service/devis-auto.service';
import { FormuleAutoService } from '../../core/service/formule-auto.service';
import { DevisAutoLayout } from '../devis-auto-layout/devis-auto-layout';

/** US-29 : constante activant la reprise des options de l'étape 2 (false = aucune option cochée par défaut). */
const REPRENDRE_OPTIONS_ETAPE_2 = false;

type Periodicite = 'mensuel' | 'annuel';

interface CarteFormule {
  code: FormuleMoteur;
  nom: string;
  description: string;
  garanties: string[];
  exclusionsSpecifiques: string[];
  conseillee: boolean;
}

interface DefinitionOption {
  code: OptionAuto;
  libelle: string;
  description: string;
  formulesCompatibles: FormuleMoteur[];
  lieABesoin: boolean;
}

const CARTES: CarteFormule[] = [
  {
    code: 'TIERS',
    nom: 'Tiers',
    description: "L'essentiel obligatoire pour rouler en règle.",
    garanties: ['Responsabilité civile', 'Défense pénale et recours'],
    exclusionsSpecifiques: ['Dommages subis par votre véhicule', 'Blessures du conducteur responsable'],
    conseillee: false,
  },
  {
    code: 'INTER',
    nom: 'Intermédiaire',
    description: "L'équilibre entre protection et budget.",
    garanties: ['Garanties du Tiers', 'Vol et incendie', 'Bris de glace', 'Événements climatiques', 'Catastrophes naturelles'],
    exclusionsSpecifiques: ['Usure ou défaut d\'entretien', 'Vol sans effraction ou clés laissées dans le véhicule', 'Conduite sous alcool ou stupéfiants (garanties dommages)'],
    conseillee: false,
  },
  {
    code: 'TR',
    nom: 'Tous risques',
    description: 'La protection la plus large, même si vous êtes responsable.',
    garanties: ['Garanties Intermédiaire', 'Dommages tous accidents'],
    exclusionsSpecifiques: ['Usure ou défaut d\'entretien', 'Vol sans effraction ou clés laissées dans le véhicule', 'Conduite sous alcool ou stupéfiants (garanties dommages)'],
    conseillee: false,
  },
];

const EXCLUSIONS_COMMUNES = [
  'Conducteur sans permis valable',
  'Dommages causés volontairement',
  'Compétitions et transport de matières dangereuses',
];

const OPTIONS: DefinitionOption[] = [
  {
    code: 'ASSISTANCE_0KM',
    libelle: 'Assistance dès 0 km',
    description: 'Dépannage même en bas de chez vous.',
    formulesCompatibles: ['TIERS', 'INTER', 'TR'],
    lieABesoin: true,
  },
  {
    code: 'VEHICULE_REMPLACEMENT',
    libelle: 'Véhicule de remplacement',
    description: "Un véhicule prêté pendant l'immobilisation du vôtre.",
    formulesCompatibles: ['INTER', 'TR'],
    lieABesoin: true,
  },
  {
    code: 'PROTECTION_CONDUCTEUR',
    libelle: 'Protection du conducteur renforcée',
    description: 'Vos blessures indemnisées, même si vous êtes responsable.',
    formulesCompatibles: ['TIERS', 'INTER', 'TR'],
    lieABesoin: true,
  },
  {
    code: 'PROTECTION_JURIDIQUE',
    libelle: 'Protection juridique',
    description: 'Accompagnement en cas de litige lié à votre véhicule.',
    formulesCompatibles: ['TIERS', 'INTER', 'TR'],
    lieABesoin: true,
  },
];

const RANG: Record<FormuleMoteur, number> = { TIERS: 0, INTER: 1, TR: 2 };

function conseilleVersMoteur(code: string | null | undefined): FormuleMoteur {
  if (code === 'INTERMEDIAIRE') return 'INTER';
  if (code === 'TOUS_RISQUES') return 'TR';
  return 'TIERS';
}

@Component({
  selector: 'iard-devis-auto-formule',
  imports: [RouterLink, DevisAutoLayout],
  templateUrl: './devis-auto-formule.html',
  styleUrl: './devis-auto-formule.scss',
})
export class DevisAutoFormule implements OnInit {
  protected readonly svc = inject(DevisAutoService);
  private readonly formuleService = inject(FormuleAutoService);
  private readonly router = inject(Router);

  protected readonly CARTES = CARTES;
  protected readonly OPTIONS = OPTIONS;
  protected readonly EXCLUSIONS_COMMUNES = EXCLUSIONS_COMMUNES;

  protected readonly periodicite = signal<Periodicite>('mensuel');
  protected readonly formuleSelectionnee = signal<FormuleMoteur>('TIERS');
  protected readonly optionsChoisies = signal<OptionAuto[]>([]);
  protected readonly exclusionsOuvertes = signal<Set<FormuleMoteur>>(new Set());
  protected readonly chargement = signal(true);
  protected readonly erreurChargement = signal(false);
  protected readonly envoi = signal(false);
  protected readonly alerteConfirmee = signal(false);
  protected readonly messageVehiculeRemplacement = signal<string | null>(null);
  protected readonly erreurSouscription = signal<string | null>(null);

  protected prixFormules = signal<PrixFormuleAuto[]>([]);
  protected prixOptions = signal<PrixOptionAuto[]>([]);
  protected versionGrille = signal<string | null>(null);

  protected readonly formuleConseilleeMoteur = computed(() =>
    conseilleVersMoteur(this.svc.recommandation()?.formuleConseillee));

  protected readonly alerteChoixMoinsCourant = computed(() => {
    const selectionne = RANG[this.formuleSelectionnee()];
    const conseille = RANG[this.formuleConseilleeMoteur()];
    return selectionne < conseille;
  });

  protected readonly cartes = computed(() =>
    CARTES.map(c => ({ ...c, conseillee: c.code === this.formuleConseilleeMoteur() })));

  protected readonly afficherAnnuel = computed(() => this.periodicite() === 'annuel');

  protected readonly unitePrix = computed(() => this.afficherAnnuel() ? '/an' : '/mois');

  ngOnInit(): void {
    const devisId = this.svc.devisAutoId();
    if (devisId === null) { this.erreurChargement.set(true); this.chargement.set(false); return; }

    this.formuleSelectionnee.set(this.formuleConseilleeMoteur());

    // Restituer les options choisies si elles existent déjà
    const deja = this.svc.formuleChoisie();
    if (deja) {
      this.formuleSelectionnee.set(deja.formuleMoteur);
      this.optionsChoisies.set([...deja.options]);
    }

    this.formuleService.obtenirPrix(devisId).subscribe(rep => {
      if (rep) {
        this.prixFormules.set(rep.prix ?? []);
        this.prixOptions.set(rep.prixOptions ?? []);
        this.versionGrille.set(rep.versionGrille ?? null);

        // REPRENDRE_OPTIONS_ETAPE_2 : cocher les options liées à un besoin si activé et première visite
        if (REPRENDRE_OPTIONS_ETAPE_2 && !deja) {
          this.optionsChoisies.set(this.optionsInitiales());
        }
      } else {
        this.erreurChargement.set(true);
      }
      this.chargement.set(false);
    });
  }

  protected choisirPeriodicite(p: Periodicite): void {
    this.periodicite.set(p);
  }

  protected choisir(code: FormuleMoteur): void {
    this.formuleSelectionnee.set(code);
    this.alerteConfirmee.set(false);

    // Passage en TIERS → décoche VEHICULE_REMPLACEMENT si coché
    if (code === 'TIERS') {
      const avecVR = this.optionsChoisies().includes('VEHICULE_REMPLACEMENT');
      if (avecVR) {
        this.optionsChoisies.update(opts => opts.filter(o => o !== 'VEHICULE_REMPLACEMENT'));
        this.messageVehiculeRemplacement.set(
          "Le véhicule de remplacement a été retiré : il n'est pas disponible avec la formule Tiers.");
      }
    } else {
      this.messageVehiculeRemplacement.set(null);
    }
  }

  protected toggleOption(code: OptionAuto): void {
    this.optionsChoisies.update(opts =>
      opts.includes(code) ? opts.filter(o => o !== code) : [...opts, code]);
  }

  protected toggleExclusions(code: FormuleMoteur): void {
    this.exclusionsOuvertes.update(s => {
      const n = new Set(s);
      if (n.has(code)) { n.delete(code); } else { n.add(code); }
      return n;
    });
  }

  protected confirmerAlerte(): void {
    this.alerteConfirmee.set(true);
  }

  protected soumettre(): void {
    if (!this.peutSouscrire()) return;
    const prix = this.calculerTotal();
    const formule = this.formuleSelectionnee();
    this.envoi.set(true);
    this.erreurSouscription.set(null);

    const choisie = {
      formuleMoteur: formule,
      options: this.optionsChoisies(),
      choixDifferentDuConseil: this.alerteChoixMoinsCourant() && this.alerteConfirmee(),
      prixMensuelTtc: prix.mensuel,
      prixAnnuelTtc: prix.annuel,
      versionGrille: this.versionGrille(),
    };

    const devisId = this.svc.devisAutoId();
    if (!devisId) { this.envoi.set(false); return; }

    this.formuleService.enregistrer(devisId, choisie).subscribe(rep => {
      this.envoi.set(false);
      if (rep) {
        this.svc.enregistrerFormule(choisie);
        this.router.navigateByUrl('/devis-auto/informations');
      } else {
        this.erreurSouscription.set('Une erreur est survenue. Veuillez réessayer.');
      }
    });
  }

  protected peutSouscrire(): boolean {
    if (this.alerteChoixMoinsCourant() && !this.alerteConfirmee()) return false;
    return !this.envoi();
  }

  protected formaterPrix(valeur: number | undefined | null): string {
    if (valeur == null) return '–';
    return valeur.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected formaterEntier(valeur: number): string {
    if (Number.isInteger(valeur)) return valeur.toLocaleString('fr-FR');
    return this.formaterPrix(valeur);
  }

  protected ipidUrl(code: FormuleMoteur): string {
    return `${apiBase('assurance-iard-api')}/documents-contrat/ipid/${code}`;
  }

  protected telechargerDevis(): void {
    const devisId = this.svc.devisAutoId();
    if (!devisId) return;
    window.open(this.formuleService.ficheConseilUrl(devisId), '_blank');
  }

  protected prixPourFormule(code: FormuleMoteur): number | null {
    const f = this.prixFormules().find(p => p.formule === code);
    if (!f) return null;
    return this.afficherAnnuel() ? f.annuel : f.mensuel;
  }

  protected prixPourOption(code: OptionAuto): number | null {
    const o = this.prixOptions().find(p => p.option === code);
    if (!o) return null;
    return this.afficherAnnuel() ? o.mensuel * 12 : o.mensuel;
  }

  protected franchisePourFormule(code: FormuleMoteur): string {
    if (code === 'TIERS') return '';
    const f = this.prixFormules().find(p => p.formule === code);
    if (!f || f.franchise == null) return '–';
    return this.formaterEntier(f.franchise) + ' €';
  }

  protected calculerTotal(): { mensuel: number | null; annuel: number | null } {
    const fp = this.prixFormules().find(p => p.formule === this.formuleSelectionnee());
    if (!fp) return { mensuel: null, annuel: null };
    const optTotal = this.optionsChoisies().reduce((s, code) => {
      const o = this.prixOptions().find(p => p.option === code);
      return s + (o ? o.mensuel : 0);
    }, 0);
    return { mensuel: fp.mensuel + optTotal, annuel: fp.annuel + optTotal * 12 };
  }

  protected totalFormate(): string {
    const t = this.calculerTotal();
    const v = this.afficherAnnuel() ? t.annuel : t.mensuel;
    return v != null ? this.formaterPrix(v) : '–';
  }

  protected optionCompatible(code: OptionAuto): boolean {
    const def = OPTIONS.find(o => o.code === code)!;
    return def.formulesCompatibles.includes(this.formuleSelectionnee());
  }

  protected badgeOption(code: OptionAuto): string | null {
    if (!REPRENDRE_OPTIONS_ETAPE_2) return this.besoinCouvertParOption(code) ? 'Selon vos besoins' : null;
    return this.besoinCouvertParOption(code) ? 'Choisie à l\'étape 2' : null;
  }

  protected statutBesoin(besoin: keyof ReturnType<DevisAutoFormule['besoinsActifs']>): { texte: string; couleur: string } {
    const formule = this.formuleSelectionnee();
    if (besoin === 'brisGlace') {
      if (formule === 'TIERS') return { texte: 'Non inclus en Tiers', couleur: 'orange' };
      return { texte: 'Inclus dans la formule', couleur: 'vert' };
    }
    const optMap: Record<string, OptionAuto> = {
      vehiculeIndispensable: 'VEHICULE_REMPLACEMENT',
      assistance0km: 'ASSISTANCE_0KM',
      protectionConducteur: 'PROTECTION_CONDUCTEUR',
      accompagnementLitige: 'PROTECTION_JURIDIQUE',
    };
    const optCode = optMap[besoin];
    if (!optCode) return { texte: '–', couleur: '' };
    if (!this.optionCompatible(optCode)) return { texte: 'Indisponible en Tiers', couleur: 'orange' };
    if (this.optionsChoisies().includes(optCode)) return { texte: 'Option ajoutée', couleur: 'vert' };
    return { texte: 'En option ci-dessous', couleur: 'rouge' };
  }

  protected besoinsActifs() {
    const b = this.svc.besoins();
    return {
      brisGlace: !!b?.brisGlace,
      vehiculeIndispensable: !!b?.vehiculeIndispensable,
      assistance0km: !!b?.assistance0km,
      protectionConducteur: !!b?.protectionConducteur,
      accompagnementLitige: !!b?.accompagnementLitige,
    };
  }

  protected lignesBesoinsActifs(): Array<{ libelle: string; cle: keyof ReturnType<DevisAutoFormule['besoinsActifs']> }> {
    const b = this.besoinsActifs();
    type Ligne = { libelle: string; cle: keyof ReturnType<DevisAutoFormule['besoinsActifs']> };
    const toutes: Ligne[] = [
      { libelle: 'Bris de glace', cle: 'brisGlace' },
      { libelle: 'Véhicule indispensable', cle: 'vehiculeIndispensable' },
      { libelle: 'Assistance dès 0 km', cle: 'assistance0km' },
      { libelle: 'Protection du conducteur', cle: 'protectionConducteur' },
      { libelle: 'Accompagnement en cas de litige', cle: 'accompagnementLitige' },
    ];
    return toutes.filter(l => b[l.cle]);
  }

  protected nomFormuleConseille(): string {
    const c = this.formuleConseilleeMoteur();
    return CARTES.find(x => x.code === c)?.nom ?? c;
  }

  protected nomFormuleSelectionnee(): string {
    return CARTES.find(x => x.code === this.formuleSelectionnee())?.nom ?? this.formuleSelectionnee();
  }

  private optionsInitiales(): OptionAuto[] {
    const b = this.svc.besoins();
    if (!b) return [];
    const compatibles: OptionAuto[] = [];
    if (b.assistance0km && this.optionCompatible('ASSISTANCE_0KM')) compatibles.push('ASSISTANCE_0KM');
    if (b.vehiculeIndispensable && this.optionCompatible('VEHICULE_REMPLACEMENT')) compatibles.push('VEHICULE_REMPLACEMENT');
    if (b.protectionConducteur && this.optionCompatible('PROTECTION_CONDUCTEUR')) compatibles.push('PROTECTION_CONDUCTEUR');
    if (b.accompagnementLitige && this.optionCompatible('PROTECTION_JURIDIQUE')) compatibles.push('PROTECTION_JURIDIQUE');
    return compatibles;
  }

  private besoinCouvertParOption(code: OptionAuto): boolean {
    const b = this.svc.besoins();
    if (!b) return false;
    return (code === 'ASSISTANCE_0KM' && !!b.assistance0km)
      || (code === 'VEHICULE_REMPLACEMENT' && !!b.vehiculeIndispensable)
      || (code === 'PROTECTION_CONDUCTEUR' && !!b.protectionConducteur)
      || (code === 'PROTECTION_JURIDIQUE' && !!b.accompagnementLitige);
  }
}
