/** US-04 : un document déposé par le client (US-13). */
export interface DocumentClient {
  id: number;
  referenceDevis: string;
  categorie: string;
  typeDocument: string;
  nomFichier: string;
  typeFichier: string;
  taille: number;
}

const LIBELLES_CATEGORIE: Record<string, string> = {
  identite: "Pièce d'identité",
  domicile: 'Justificatif de domicile',
};

export function libelleCategorie(categorie: string): string {
  return LIBELLES_CATEGORIE[categorie] ?? categorie;
}
