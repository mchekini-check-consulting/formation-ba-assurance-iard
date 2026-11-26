import type { Produit } from './contrat';

/** US-24 : un contrat ouvert à la déclaration (AC1). */
export interface ContratDeclarable {
  numero: string;
  produit: Produit;
  statut: string;
  formule: string | null;
  bienAssure: string | null;
  dateEffet: string;
  dateFin: string | null;
}

/** US-24, AC1 et AC10 : de quoi commencer une déclaration. */
export interface ContexteDeclaration {
  prenom: string | null;
  numeroAssistance: string;
  contrats: ContratDeclarable[];
}

export interface PieceAttendue {
  code: string;
  libelle: string;
  obligatoire: boolean;
}

/** US-24, AC2, RG1 : un type de sinistre du produit (« Autre » toujours en dernier). */
export interface TypeSinistre {
  code: string;
  libelle: string;
  aide: string | null;
  delaiPrevu: string | null;
  demandeDateArrete: boolean;
  gestesUrgence: string[];
  pieces: PieceAttendue[];
}

export interface TypesSinistre {
  numeroContrat: string;
  produit: Produit;
  numeroAssistance: string;
  types: TypeSinistre[];
}

export interface Tiers {
  nom: string | null;
  immatriculation: string | null;
  assureur: string | null;
}

export type Declarant = 'SOUSCRIPTEUR' | 'CONDUCTEUR' | 'PROCHE';

/** Les réponses d'une déclaration, telles qu'elles sont enregistrées dans le brouillon (AC8). */
export interface ReponsesSinistre {
  type: string;
  etape?: number;
  dateSurvenance: string | null;
  heureSurvenance: string | null;
  lieu: string | null;
  description: string | null;
  tiersImplique: boolean | null;
  tiers: Tiers | null;
  blesses: boolean | null;
  nombreBlesses: number | null;
  declarant: Declarant | null;
  declarantNom: string | null;
  declarantLien: string | null;
  details: Record<string, string>;
  dateArreteCatnat: string | null;
  explicationRetard: string | null;
}

/** AC3, AC5, AC6 : contrôles recalculés par le serveur à chaque enregistrement. */
export interface Controles {
  /** null pour « Autre » : aucun message. */
  couvert: boolean | null;
  horsPeriodeContrat: boolean;
  horsDelai: boolean;
  delaiPrevu: string | null;
  dateLimite: string | null;
}

export interface PieceJointe {
  id: number;
  typePiece: string;
  nomFichier: string;
  format: string;
  tailleOctets: number;
  deposeeLe: string | null;
}

export interface DeclarationSinistre extends ReponsesSinistre {
  id: number;
  numero: string | null;
  statut: 'BROUILLON' | 'DECLARE';
  etape: number;
  prenom: string | null;
  numeroAssistance: string;
  contrat: ContratDeclarable;
  libelleType: string;
  gestesUrgence: string[];
  controles: Controles;
  piecesAttendues: PieceAttendue[];
  piecesJointes: PieceJointe[];
  piecesManquantes: boolean;
  brouillonExpireLe: string | null;
  declareLe: string | null;
}

/** AC9 : dossier créé. */
export interface SinistreEnvoye {
  id: number;
  numero: string;
  statut: string;
  declareLe: string;
  piecesManquantes: boolean;
  emailEnvoye: boolean;
}

/** « Mes sinistres » : un dossier ou un brouillon. */
export interface SinistreResume {
  id: number;
  numero: string | null;
  statut: 'BROUILLON' | 'DECLARE';
  type: string;
  libelleType: string;
  numeroContrat: string;
  produit: Produit;
  dateSurvenance: string | null;
  etape: number;
  piecesManquantes: boolean;
  declareLe: string | null;
  brouillonExpireLe: string | null;
}

export const AUTRE = 'AUTRE';
export const TAILLE_MAX_OCTETS = 10 * 1024 * 1024;
export const NOMBRE_MAX_PIECES = 20;
export const EXTENSIONS_ACCEPTEES = ['pdf', 'jpg', 'jpeg', 'png'];
export const ACCEPT_FICHIERS = '.pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png';

/** Chemins des étapes, sous /espace-client/sinistres/:id. */
export const ETAPES = ['type', 'circonstances', 'justificatifs'] as const;
export const ADRESSE_SINISTRES = '/espace-client/sinistres';
export const ADRESSE_DECLARER = '/espace-client/sinistres/declarer';

/** AC8 : la page où reprendre un brouillon (étape 1 à 3) ; un dossier envoyé ouvre ses justificatifs. */
export function lienReprise(sinistre: { id: number; statut: string; etape: number }): string[] {
  if (sinistre.statut !== 'BROUILLON') return [ADRESSE_SINISTRES, String(sinistre.id), 'justificatifs'];
  const etape = Math.min(Math.max(sinistre.etape, 1), ETAPES.length);
  return [ADRESSE_SINISTRES, String(sinistre.id), ETAPES[etape - 1]];
}

/** AC13 : qui déclare (le conducteur secondaire n'existe qu'en auto). */
export function declarantsPossibles(produit: Produit): { valeur: Declarant; libelle: string }[] {
  return [
    { valeur: 'SOUSCRIPTEUR', libelle: 'Moi-même, l’assuré' },
    ...(produit === 'AUTO' ? [{ valeur: 'CONDUCTEUR' as const, libelle: 'Le conducteur secondaire déclaré' }] : []),
    { valeur: 'PROCHE', libelle: 'Un proche, pour le compte de l’assuré' },
  ];
}

export const LIBELLES_DECLARANT: Record<Declarant, string> = {
  SOUSCRIPTEUR: 'L’assuré lui-même',
  CONDUCTEUR: 'Le conducteur secondaire déclaré',
  PROCHE: 'Un proche de l’assuré',
};

/** AC4 : une question propre au type de sinistre ; la réponse est enregistrée sous le libellé de la question. */
export interface Question {
  libelle: string;
  genre: 'oui-non' | 'choix' | 'texte';
  options?: string[];
}

const OUI_NON = (libelle: string): Question => ({ libelle, genre: 'oui-non' });
const PLAINTE = OUI_NON('Avez-vous déposé plainte ?');
const ROULANT = OUI_NON('Le véhicule peut-il encore rouler ?');
const POMPIERS = OUI_NON('Les pompiers sont-ils intervenus ?');
const EVENEMENT: Question = {
  libelle: 'Quel événement ?',
  genre: 'choix',
  options: ['Tempête', 'Grêle', 'Inondation', 'Sécheresse', 'Autre événement'],
};

const QUESTIONS: Record<Produit, Record<string, Question[]>> = {
  AUTO: {
    ACCIDENT_TIERS: [OUI_NON('Un constat amiable a-t-il été rempli ?'), ROULANT],
    ACCIDENT_SANS_TIERS: [ROULANT, { libelle: 'Qu’a heurté le véhicule ?', genre: 'texte' }],
    VOL: [PLAINTE, OUI_NON('Le véhicule a-t-il été retrouvé ?')],
    BRIS_GLACE: [
      {
        libelle: 'Quel vitrage est touché ?',
        genre: 'choix',
        options: ['Pare-brise', 'Vitre latérale', 'Lunette arrière', 'Toit vitré', 'Autre vitrage'],
      },
    ],
    INCENDIE: [POMPIERS],
    CLIMATIQUE_CATNAT: [EVENEMENT],
  },
  HABITATION: {
    DEGAT_EAUX: [
      {
        libelle: 'D’où vient l’eau ?',
        genre: 'choix',
        options: ['Fuite d’une canalisation', 'Infiltration', 'Débordement', 'Logement voisin', 'Je ne sais pas'],
      },
      OUI_NON('Un voisin ou un tiers est-il touché ?'),
      { libelle: 'Quelles pièces sont touchées ?', genre: 'texte' },
    ],
    VOL_CAMBRIOLAGE: [PLAINTE, OUI_NON('Y a-t-il eu effraction ?')],
    INCENDIE: [POMPIERS],
    BRIS_GLACE: [
      {
        libelle: 'Quel vitrage est touché ?',
        genre: 'choix',
        options: ['Fenêtre', 'Baie vitrée', 'Véranda', 'Porte vitrée', 'Autre vitrage'],
      },
    ],
    CLIMATIQUE_CATNAT: [EVENEMENT],
    RC_TIERS: [
      {
        libelle: 'Quel dommage avez-vous causé ?',
        genre: 'choix',
        options: ['Dommage à un bien', 'Dommage à une personne', 'Les deux'],
      },
    ],
  },
};

/** AC4 : questions affichées pour ce type ; aucune pour « Autre » (description libre, AC12). */
export function questionsDuType(produit: Produit, type: string): Question[] {
  return QUESTIONS[produit]?.[type] ?? [];
}

/** AC10 : « Demander un dépannage » pour un accident auto (en attendant l'écran d'assistance d'US-47). */
export function proposeDepannage(produit: Produit, type: string | null): boolean {
  return produit === 'AUTO' && (type === 'ACCIDENT_TIERS' || type === 'ACCIDENT_SANS_TIERS');
}

/** Lien d'appel du numéro d'assistance (chiffres seulement). */
export function lienTelephone(numero: string): string {
  return `tel:${numero.replace(/[^\d+]/g, '')}`;
}

// --- Messages (AC3, AC5, AC6, AC11, AC12, RG8) ----------------------------------------------------------------

export function messageNonCouvert(formule: string): string {
  return (
    `Ce type de sinistre n’est pas couvert par votre formule ${formule} à la date du sinistre. ` +
    'Vous pouvez quand même le déclarer : votre gestionnaire étudiera votre dossier.'
  );
}

export const MESSAGE_HORS_PERIODE =
  'Votre contrat n’était pas actif à cette date. Vous pouvez continuer : votre dossier sera signalé à votre gestionnaire.';

export function messageDelai(delaiPrevu: string | null): string {
  const delai = delaiPrevu ? ` est de ${delaiPrevu}` : ' est dépassé';
  return (
    `Le délai prévu pour déclarer ce type de sinistre${delai}. ` +
    'Votre déclaration reste possible : expliquez-nous en quelques mots la raison du délai.'
  );
}

export const MESSAGE_BLESSES =
  'Un gestionnaire spécialisé vous contactera. N’envoyez aucune information médicale par e-mail.';

export const MESSAGE_AUTRE =
  'Décrivez librement ce qui s’est passé : votre gestionnaire choisira le bon type de sinistre.';

export const MESSAGE_POUR_UN_AUTRE =
  'L’assuré sera informé de cette déclaration par e-mail. Il reste le seul à pouvoir accepter une offre.';

export const TEXTE_HONNEUR =
  'Je certifie sur l’honneur que les informations de cette déclaration sont exactes. ' +
  'Je sais qu’une fausse déclaration intentionnelle entraîne la perte du droit à indemnité.';

export const MESSAGE_TAILLE = 'Ce fichier dépasse 10 Mo : déposez un fichier plus léger (PDF, JPG ou PNG de 10 Mo maximum).';
export const MESSAGE_FORMAT = 'Ce fichier n’est pas accepté : déposez un PDF, un JPG ou un PNG.';
export const MESSAGE_NOMBRE = 'Vous avez déjà déposé 20 fichiers, le maximum pour un dossier.';

/** AC15 : contrôle avant l'envoi du fichier (le serveur contrôle aussi le contenu réel). */
export function refusFichier(fichier: { name: string; size: number }, dejaDeposees: number): string | null {
  if (dejaDeposees >= NOMBRE_MAX_PIECES) return MESSAGE_NOMBRE;
  const extension = fichier.name.includes('.') ? fichier.name.split('.').pop()!.toLowerCase() : '';
  if (!EXTENSIONS_ACCEPTEES.includes(extension)) return MESSAGE_FORMAT;
  if (fichier.size > TAILLE_MAX_OCTETS) return MESSAGE_TAILLE;
  return null;
}

// --- Étape 2 : réponses obligatoires ----------------------------------------------------------------------------

export type ChampObligatoire =
  | 'dateSurvenance'
  | 'lieu'
  | 'description'
  | 'tiersImplique'
  | 'blesses'
  | 'nombreBlesses'
  | 'declarant'
  | 'declarantNom'
  | 'declarantLien'
  | 'explicationRetard';

const MESSAGES_OBLIGATOIRES: Record<ChampObligatoire, string> = {
  dateSurvenance: 'Indiquez la date du sinistre.',
  lieu: 'Indiquez le lieu du sinistre.',
  description: 'Décrivez ce qui s’est passé.',
  tiersImplique: 'Indiquez si un tiers est impliqué.',
  blesses: 'Indiquez s’il y a des blessés.',
  nombreBlesses: 'Indiquez le nombre de blessés.',
  declarant: 'Indiquez qui déclare le sinistre.',
  declarantNom: 'Indiquez votre nom.',
  declarantLien: 'Indiquez votre lien avec l’assuré.',
  explicationRetard: 'Expliquez en quelques mots la raison du délai de déclaration.',
};

export const MESSAGE_DATE_FUTURE = 'La date du sinistre ne peut pas être dans le futur.';

function vide(valeur: string | null | undefined): boolean {
  return !valeur || !valeur.trim();
}

/**
 * AC4, AC6, AC11, AC13 : messages des réponses obligatoires manquantes de l'étape 2 (vide si tout est rempli).
 * `aujourdhui` au format AAAA-MM-JJ.
 */
export function erreursCirconstances(
  reponses: ReponsesSinistre,
  aujourdhui: string,
  explicationDemandee: boolean,
): Partial<Record<ChampObligatoire, string>> {
  const pourUnAutre = !!reponses.declarant && reponses.declarant !== 'SOUSCRIPTEUR';
  const manque: Record<ChampObligatoire, boolean> = {
    dateSurvenance: !reponses.dateSurvenance,
    lieu: vide(reponses.lieu),
    description: vide(reponses.description),
    tiersImplique: reponses.tiersImplique === null,
    blesses: reponses.blesses === null,
    nombreBlesses: reponses.blesses === true && !reponses.nombreBlesses,
    declarant: !reponses.declarant,
    declarantNom: pourUnAutre && vide(reponses.declarantNom),
    declarantLien: pourUnAutre && vide(reponses.declarantLien),
    explicationRetard: explicationDemandee && vide(reponses.explicationRetard),
  };
  const erreurs: Partial<Record<ChampObligatoire, string>> = Object.fromEntries(
    (Object.keys(manque) as ChampObligatoire[])
      .filter((champ) => manque[champ])
      .map((champ) => [champ, MESSAGES_OBLIGATOIRES[champ]]),
  );
  if (reponses.dateSurvenance && reponses.dateSurvenance > aujourdhui) {
    erreurs.dateSurvenance = MESSAGE_DATE_FUTURE;
  }
  return erreurs;
}

/** Les réponses d'une déclaration, prêtes à être renvoyées au serveur (toutes, à chaque enregistrement). */
export function reponsesDe(declaration: DeclarationSinistre): ReponsesSinistre {
  return {
    type: declaration.type,
    etape: declaration.etape,
    dateSurvenance: declaration.dateSurvenance,
    heureSurvenance: declaration.heureSurvenance ? declaration.heureSurvenance.slice(0, 5) : null,
    lieu: declaration.lieu,
    description: declaration.description,
    tiersImplique: declaration.tiersImplique,
    tiers: declaration.tiers,
    blesses: declaration.blesses,
    nombreBlesses: declaration.nombreBlesses,
    declarant: declaration.declarant,
    declarantNom: declaration.declarantNom,
    declarantLien: declaration.declarantLien,
    details: { ...declaration.details },
    dateArreteCatnat: declaration.dateArreteCatnat,
    explicationRetard: declaration.explicationRetard,
  };
}

// --- Mise en forme ----------------------------------------------------------------------------------------------

/** « 2026-11-24 » → « 24/11/2026 ». */
export function jourFrancais(date: string | null | undefined): string {
  if (!date) return '';
  const [annee, mois, jour] = date.slice(0, 10).split('-');
  return `${jour}/${mois}/${annee}`;
}

/** « 08:30:00 » → « 08:30 ». */
export function heureFrancaise(heure: string | null | undefined): string {
  return heure ? heure.slice(0, 5) : '';
}

/** Date du jour, heure de Paris, au format AAAA-MM-JJ (limite des champs date). */
export function aujourdhuiIso(maintenant: Date = new Date()): string {
  return maintenant.toLocaleDateString('sv-SE', { timeZone: 'Europe/Paris' });
}

/** Taille lisible d'un fichier (« 2,1 Mo »). */
export function taille(octets: number): string {
  if (octets < 1024 * 1024) return `${Math.max(1, Math.round(octets / 1024))} Ko`;
  return `${(octets / (1024 * 1024)).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} Mo`;
}
