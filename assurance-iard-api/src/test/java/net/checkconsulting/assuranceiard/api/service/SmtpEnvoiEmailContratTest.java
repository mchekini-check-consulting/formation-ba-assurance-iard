package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SmtpEnvoiEmailContratTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEnvoiEmailContrat service;

    @BeforeEach
    void setUp() {
        service = new SmtpEnvoiEmailContrat(mailSender);
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@assureasy.fr");
    }

    @Test
    void envoyeUnEmailAvecLeCodeAuDestinataire() {
        service.envoyer("client@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("noreply@assureasy.fr");
        assertThat(message.getTo()).containsExactly("client@example.com");
        assertThat(message.getSubject()).contains("signature");
        assertThat(message.getText()).contains("123456");
    }
}
