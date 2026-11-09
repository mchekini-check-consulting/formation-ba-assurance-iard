package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.checkconsulting.assuranceiard.api.dto.CompteResponse;
import net.checkconsulting.assuranceiard.api.dto.CreationCompteRequest;
import net.checkconsulting.assuranceiard.api.exception.EmailDejaUtiliseException;
import net.checkconsulting.assuranceiard.api.mapper.CompteMapper;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CompteServiceTest {

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CompteService compteService;

    @BeforeEach
    void preparer() {
        compteService = new CompteService(compteRepository, passwordEncoder, Mappers.getMapper(CompteMapper.class));
    }

    @Test
    void creeLeCompteAvecUnEmailNormaliseEtUnMotDePasseChiffre() {
        when(compteRepository.existsByEmail("marie@exemple.fr")).thenReturn(false);
        when(passwordEncoder.encode("Abcdefg1")).thenReturn("empreinte");
        when(compteRepository.saveAndFlush(any(Compte.class))).thenAnswer(appel -> {
            Compte compte = appel.getArgument(0);
            compte.setId(7L);
            return compte;
        });

        CompteResponse reponse = compteService.creer(new CreationCompteRequest(" Marie@Exemple.fr ", "Abcdefg1"));

        assertThat(reponse).isEqualTo(new CompteResponse(7L, "marie@exemple.fr"));
        ArgumentCaptor<Compte> enregistre = ArgumentCaptor.forClass(Compte.class);
        verify(compteRepository).saveAndFlush(enregistre.capture());
        assertThat(enregistre.getValue().getMotDePasseHash()).isEqualTo("empreinte");
    }

    @Test
    void refuseUnEmailDejaUtilise() {
        when(compteRepository.existsByEmail("deja@exemple.fr")).thenReturn(true);

        assertThatThrownBy(() -> compteService.creer(new CreationCompteRequest("DEJA@exemple.fr", "Abcdefg1")))
                .isInstanceOf(EmailDejaUtiliseException.class)
                .hasMessage("Un compte existe déjà avec cette adresse.");
        verify(compteRepository, never()).saveAndFlush(any());
    }

    @Test
    void refuseLeDoublonDetecteParLaBase() {
        when(compteRepository.existsByEmail("marie@exemple.fr")).thenReturn(false);
        when(passwordEncoder.encode("Abcdefg1")).thenReturn("empreinte");
        when(compteRepository.saveAndFlush(any(Compte.class)))
                .thenThrow(new DataIntegrityViolationException("uk_compte_email"));

        assertThatThrownBy(() -> compteService.creer(new CreationCompteRequest("marie@exemple.fr", "Abcdefg1")))
                .isInstanceOf(EmailDejaUtiliseException.class);
    }
}
