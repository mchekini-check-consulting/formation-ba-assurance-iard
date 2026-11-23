// US-14, RG2 : même contrôle que IbanService côté backend, pour un message immédiat à l'écran.

const FORMAT = /^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/;
const LONGUEUR_FRANCE = 27;
const LONGUEUR_ENTETE = 4;
const TAILLE_GROUPE = 4;
const MODULE = 97;
const CODE_A = 'A'.charCodeAt(0);
const VALEUR_A = 10;

export function normaliserIban(texte: string): string {
  return texte.replace(/\s/g, '').toUpperCase();
}

/** Affiche l'IBAN par groupes de 4, comme sur un relevé d'identité bancaire. */
export function formaterIban(texte: string): string {
  return normaliserIban(texte).replace(new RegExp(`(.{${TAILLE_GROUPE}})(?=.)`, 'g'), '$1 ');
}

// Modulo 97 : les 4 premiers caractères passent à la fin, chaque lettre devient un nombre de 10 à 35
function cleValide(iban: string): boolean {
  const reordonne = iban.slice(LONGUEUR_ENTETE) + iban.slice(0, LONGUEUR_ENTETE);
  const chiffres = [...reordonne].map((c) => (/\d/.test(c) ? c : String(c.charCodeAt(0) - CODE_A + VALEUR_A)));
  return [...chiffres.join('')].reduce((reste, chiffre) => (reste * 10 + Number(chiffre)) % MODULE, 0) === 1;
}

export function ibanValide(texte: string): boolean {
  const iban = normaliserIban(texte);
  const longueurValide = !iban.startsWith('FR') || iban.length === LONGUEUR_FRANCE;
  return FORMAT.test(iban) && longueurValide && cleValide(iban);
}

export function erreurIban(texte: string): string | null {
  if (!texte.trim()) return 'Indiquez votre IBAN.';
  return ibanValide(texte) ? null : "Cet IBAN n'est pas valide. Vérifiez sa saisie.";
}

// AC1 : seuls les 4 derniers caractères restent visibles
export function ibanMasque(texte: string): string {
  return ibanMasqueDepuisFin(normaliserIban(texte).slice(-TAILLE_GROUPE));
}

/** US-04, RG4 : même masquage pour tous les profils, à partir des 4 derniers caractères envoyés par le serveur. */
export function ibanMasqueDepuisFin(fin: string | null | undefined): string {
  return `•••• •••• •••• ${fin ?? '••••'}`;
}
