package net.checkconsulting.assuranceiard.api.service;

import net.checkconsulting.assuranceiard.api.dto.CompteResponse;
import net.checkconsulting.assuranceiard.api.dto.CreationCompteRequest;
import net.checkconsulting.assuranceiard.api.exception.EmailDejaUtiliseException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.mapper.CompteMapper;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompteService {

    private final CompteRepository compteRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompteMapper compteMapper;

    @LogMetier
    public CompteResponse creer(CreationCompteRequest demande) {
        String email = demande.email().trim().toLowerCase(Locale.ROOT);
        if (compteRepository.existsByEmail(email)) {
            throw new EmailDejaUtiliseException();
        }
        Compte compte = new Compte();
        compte.setEmail(email);
        compte.setMotDePasseHash(passwordEncoder.encode(demande.motDePasse()));
        try {
            return compteMapper.versReponse(compteRepository.saveAndFlush(compte));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailDejaUtiliseException();
        }
    }
}
