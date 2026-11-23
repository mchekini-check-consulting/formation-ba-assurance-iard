package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import org.junit.jupiter.api.Test;

class CatalogueModelesTest {

    /** Un modèle de test : il convient aux documents dont la référence vaut {@code reference}. */
    private record Modele(TypeDocument type, String reference) implements ModeleDocument {

        @Override
        public boolean convientA(Document document) {
            return reference.equals(document.getReference());
        }

        @Override
        public ContenuDocument rediger(Document document) {
            return new ContenuDocument("titre", "1", List.of());
        }
    }

    private static Document document(TypeDocument type, String reference) {
        Document document = new Document();
        document.setType(type.name());
        document.setReference(reference);
        return document;
    }

    @Test
    void choisitLeModeleDuTypeQuiConvient() {
        Modele habitation = new Modele(TypeDocument.CONDITIONS_PARTICULIERES, "H");
        Modele auto = new Modele(TypeDocument.CONDITIONS_PARTICULIERES, "A");
        CatalogueModeles catalogue = new CatalogueModeles(List.of(habitation, auto));

        assertThat(catalogue.pour(document(TypeDocument.CONDITIONS_PARTICULIERES, "A"))).contains(auto);
        assertThat(catalogue.pour(document(TypeDocument.FACTURE, "A"))).isEmpty();
        assertThat(catalogue.typesGeres()).containsExactly("CONDITIONS_PARTICULIERES");
    }

    @Test
    void deuxModelesPourLeMemeDocumentSontUneErreurDeDeveloppement() {
        CatalogueModeles catalogue = new CatalogueModeles(List.of(
                new Modele(TypeDocument.FACTURE, "X"), new Modele(TypeDocument.FACTURE, "X")));

        assertThatThrownBy(() -> catalogue.pour(document(TypeDocument.FACTURE, "X")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("FACTURE");
    }

    @Test
    void laDemandeDUnDocumentSansContratExigeUneReference() {
        assertThat(DemandeDocument.commun(TypeDocument.IPID, "TIERS", "m").contratId()).isNull();
        assertThat(DemandeDocument.duContrat(TypeDocument.MEMO_VEHICULE, 4L, "m").reference()).isNull();
        assertThatThrownBy(() -> new DemandeDocument(TypeDocument.IPID, null, null, "m"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DemandeDocument(null, 1L, null, "m")).isInstanceOf(NullPointerException.class);
    }
}
