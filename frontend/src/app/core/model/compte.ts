export interface DemandeCreationCompte {
  email: string;
  motDePasse: string;
}

export interface Compte {
  id: number;
  email: string;
}

export type ResultatCreationCompte = 'cree' | 'email-deja-utilise' | 'erreur-technique';

export interface RegleMotDePasse {
  libelle: string;
  respectee: boolean;
}

export function emailValide(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

export function reglesMotDePasse(motDePasse: string): RegleMotDePasse[] {
  return [
    { libelle: '8 caractères minimum', respectee: motDePasse.length >= 8 },
    { libelle: 'Au moins une majuscule', respectee: /[A-Z]/.test(motDePasse) },
    { libelle: 'Au moins un chiffre', respectee: /\d/.test(motDePasse) },
  ];
}

export function motDePasseConforme(motDePasse: string): boolean {
  return reglesMotDePasse(motDePasse).every((regle) => regle.respectee);
}
