/**
 * US-18 : prénom et nom affichés avec une majuscule initiale, même saisis en majuscules ou en minuscules
 * (« INES » → « Ines », « jean-pierre » → « Jean-Pierre », « o'neil » → « O'Neil »). Un accent absent de la saisie
 * ne peut pas être deviné (« INES » reste « Ines »).
 */
export function formaterNomPropre(valeur: string | null | undefined): string {
  const texte = (valeur ?? '').trim().toLocaleLowerCase('fr-FR');
  return texte.replace(/(^|[\s\-'’])(\p{L})/gu, (_tout, separateur: string, lettre: string) =>
    separateur + lettre.toLocaleUpperCase('fr-FR'),
  );
}

/** Initiales du rond de l'en-tête : première lettre du prénom et du nom (« Camille Durand » → « CD »). */
export function initiales(prenom: string | null | undefined, nom: string | null | undefined): string {
  return [prenom, nom]
    .map((partie) => (partie ?? '').trim().charAt(0).toLocaleUpperCase('fr-FR'))
    .join('');
}
