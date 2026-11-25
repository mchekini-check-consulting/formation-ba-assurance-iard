/** Un morceau de texte ; `plaque` : une immatriculation, à ne jamais couper sur deux lignes. */
export interface MorceauTexte {
  texte: string;
  plaque: boolean;
}

/** Immatriculation au format SIV (AA-123-AA). */
const PLAQUE = /[A-Z]{2}-\d{3}-[A-Z]{2}/g;

/**
 * US-21 (recette) : découpe un texte pour que les immatriculations restent d'un seul tenant (« Peugeot 208 ·
 * EF-003-GH »). Le texte reconstitué est identique à l'original.
 */
export function decouperPlaques(texte: string | null | undefined): MorceauTexte[] {
  const source = texte ?? '';
  const morceaux: MorceauTexte[] = [];
  let debut = 0;
  for (const trouve of source.matchAll(PLAQUE)) {
    const position = trouve.index ?? 0;
    if (position > debut) {
      morceaux.push({ texte: source.slice(debut, position), plaque: false });
    }
    morceaux.push({ texte: trouve[0], plaque: true });
    debut = position + trouve[0].length;
  }
  if (debut < source.length) {
    morceaux.push({ texte: source.slice(debut), plaque: false });
  }
  return morceaux;
}
