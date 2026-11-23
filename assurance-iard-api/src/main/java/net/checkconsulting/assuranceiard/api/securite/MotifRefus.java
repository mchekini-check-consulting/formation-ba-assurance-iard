package net.checkconsulting.assuranceiard.api.securite;

/** US-04, RG3 : raison d'un accès refusé, enregistrée dans le journal. */
public enum MotifRefus {
    /** Le profil n'a pas le droit demandé (ou l'administrateur l'a retiré). */
    DROIT_ABSENT,
    /** La ressource appartient à un autre client. */
    PAS_PROPRIETAIRE,
    /** Back-office demandé par un client (Prospect, Assuré). */
    BACK_OFFICE
}
