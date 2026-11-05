package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.checkconsulting.assuranceiard.api.config.JustificatifsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StockageJustificatifsTest {

    @TempDir
    private Path dossier;

    private StockageJustificatifs stockage(Path chemin) {
        return new StockageJustificatifs(new JustificatifsProperties(chemin.toString()));
    }

    @Test
    void creeLeDossierEtRangeLeFichierSousUnNomGenere() throws IOException {
        Path sousDossier = dossier.resolve("justificatifs");

        String cle = stockage(sousDossier).stocker(new byte[] {1, 2, 3}, "png");

        assertThat(cle).matches("[0-9a-f-]{36}\\.png");
        assertThat(Files.readAllBytes(sousDossier.resolve(cle))).containsExactly(1, 2, 3);
    }

    @Test
    void supprimeUnFichierEtIgnoreUnFichierDejaAbsent() {
        StockageJustificatifs stockage = stockage(dossier);
        String cle = stockage.stocker(new byte[] {1}, "pdf");

        stockage.supprimer(cle);
        stockage.supprimer(cle);

        assertThat(dossier.resolve(cle)).doesNotExist();
    }

    @Test
    void signaleUnDossierImpossibleACreer() throws IOException {
        Path fichier = Files.createFile(dossier.resolve("pas-un-dossier"));

        assertThatThrownBy(() -> stockage(fichier).stocker(new byte[] {1}, "pdf"))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void signaleUneSuppressionImpossible() throws IOException {
        Path sousDossier = Files.createDirectory(dossier.resolve("occupe"));
        Files.createFile(sousDossier.resolve("contenu"));

        assertThatThrownBy(() -> stockage(dossier).supprimer("occupe"))
                .isInstanceOf(UncheckedIOException.class);
    }
}
