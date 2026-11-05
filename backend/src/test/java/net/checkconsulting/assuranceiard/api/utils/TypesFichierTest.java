package net.checkconsulting.assuranceiard.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TypesFichierTest {

    @ParameterizedTest
    @ValueSource(strings = {"carte.pdf", "photo.jpg", "photo.jpeg", "scan.png", "PHOTO.JPG", "ma.carte.Pdf"})
    void accepteLesExtensionsPrevues(String nom) {
        assertThat(TypesFichier.extensionAcceptee(nom)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"virus.exe", "carte.pdf.exe", "photo.heic", "sansextension", ""})
    void refuseLesAutresExtensions(String nom) {
        assertThat(TypesFichier.extensionAcceptee(nom)).isFalse();
    }

    @Test
    void refuseUnFichierSansNom() {
        assertThat(TypesFichier.extensionAcceptee(null)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "25,50,44,46,2D,31, pdf",
        "FF,D8,FF,E0,00,10, jpg",
    })
    void reconnaitLeVraiType(String a, String b, String c, String d, String e, String f, String attendu) {
        byte[] contenu = {octet(a), octet(b), octet(c), octet(d), octet(e), octet(f)};

        assertThat(TypesFichier.typeReel(contenu)).contains(attendu);
    }

    @Test
    void reconnaitUnPng() {
        byte[] contenu = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};

        assertThat(TypesFichier.typeReel(contenu)).contains("png");
    }

    @Test
    void refuseUnContenuQuiNeCorrespondAAucunType() {
        assertThat(TypesFichier.typeReel("un simple texte".getBytes(StandardCharsets.UTF_8))).isEmpty();
    }

    @Test
    void refuseUnFichierTropCourtPourEtreReconnu() {
        assertThat(TypesFichier.typeReel(new byte[] {(byte) 0xFF})).isEmpty();
        assertThat(TypesFichier.typeReel(new byte[0])).isEmpty();
    }

    private byte octet(String hexadecimal) {
        return (byte) Integer.parseInt(hexadecimal, 16);
    }
}
