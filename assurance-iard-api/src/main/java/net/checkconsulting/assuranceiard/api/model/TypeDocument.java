package net.checkconsulting.assuranceiard.api.model;

/** T-05 : les 12 valeurs autorisées de document.type, avec le libellé affiché dans le pied de page des PDF. */
public enum TypeDocument {
    CONDITIONS_PARTICULIERES("Conditions particulières"),
    CONDITIONS_GENERALES("Conditions générales"),
    IPID("Document d'information (IPID)"),
    FICHE_CONSEIL("Fiche conseil"),
    MANDAT_SEPA("Mandat de prélèvement SEPA"),
    ECHEANCIER("Échéancier"),
    FACTURE("Facture"),
    AVENANT("Avenant"),
    MEMO_VEHICULE("Mémo véhicule assuré"),
    ATTESTATION("Attestation"),
    DEVIS("Devis"),
    DECLARATION_SINISTRE("Déclaration de sinistre");

    private final String libelle;

    TypeDocument(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
