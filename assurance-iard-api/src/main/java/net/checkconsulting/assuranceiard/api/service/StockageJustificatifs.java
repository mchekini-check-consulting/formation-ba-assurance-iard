package net.checkconsulting.assuranceiard.api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.config.JustificatifsProperties;
import org.springframework.stereotype.Service;

/** Range les fichiers sous un nom généré : le nom donné par le client n'entre jamais dans le chemin. */
@Service
public class StockageJustificatifs {

    private final Path dossier;

    public StockageJustificatifs(JustificatifsProperties reglages) {
        this.dossier = Path.of(reglages.dossier());
    }

    public String stocker(byte[] contenu, String extension) {
        String cle = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(dossier);
            Files.write(dossier.resolve(cle), contenu);
        } catch (IOException erreur) {
            throw new UncheckedIOException(erreur);
        }
        return cle;
    }

    /** US-04 : contenu d'un fichier ; vide s'il n'existe plus (en ligne, le dossier est vidé au redémarrage). */
    public Optional<byte[]> lire(String cle) {
        Path fichier = dossier.resolve(cle);
        if (!Files.isRegularFile(fichier)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(fichier));
        } catch (IOException erreur) {
            throw new UncheckedIOException(erreur);
        }
    }

    public void supprimer(String cle) {
        try {
            Files.deleteIfExists(dossier.resolve(cle));
        } catch (IOException erreur) {
            throw new UncheckedIOException(erreur);
        }
    }
}
