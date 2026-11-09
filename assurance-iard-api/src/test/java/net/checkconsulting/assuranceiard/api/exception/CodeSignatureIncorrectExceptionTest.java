package net.checkconsulting.assuranceiard.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodeSignatureIncorrectExceptionTest {

    @Test
    void constructeurSansArgumentRetourneEssaisRestantsMoinsUn() {
        CodeSignatureIncorrectException ex = new CodeSignatureIncorrectException();
        assertThat(ex.getEssaisRestants()).isEqualTo(-1);
    }

    @Test
    void constructeurAvecEssaisRestantsLesConserve() {
        CodeSignatureIncorrectException ex = new CodeSignatureIncorrectException(2);
        assertThat(ex.getEssaisRestants()).isEqualTo(2);
        assertThat(ex.getMessage()).contains("2");
    }
}
