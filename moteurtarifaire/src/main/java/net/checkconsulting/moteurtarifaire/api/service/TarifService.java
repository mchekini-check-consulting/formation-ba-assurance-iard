package net.checkconsulting.moteurtarifaire.api.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationRequest;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationResponse;
import net.checkconsulting.moteurtarifaire.api.exception.DonneeInvalideException;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleIndisponibleException;
import net.checkconsulting.moteurtarifaire.api.exception.TarifExpireException;
import net.checkconsulting.moteurtarifaire.api.exception.TarifInconnuException;
import net.checkconsulting.moteurtarifaire.api.mapper.TarifMapper;
import net.checkconsulting.moteurtarifaire.api.logging.LogMetier;
import net.checkconsulting.moteurtarifaire.api.dto.Besoins;
import net.checkconsulting.moteurtarifaire.api.dto.FormuleConseillee;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Orientation;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TarifService {

    private static final String CHAMP_FRANCHISE = "franchise";
    private static final String CHAMP_CODE_POSTAL = "codePostal";
    private static final String CODE_POSTAL_NON_COUVERT = "Ce code postal ne correspond à aucun département couvert.";

    private final GrilleTarifaireService grilles;
    private final EligibiliteService eligibilite;
    private final CalculateurPrix calculateurPrix;
    private final RecommandationService recommandation;
    private final TraceCalculService trace;
    private final TarifMapper mapper;
    private final Clock horloge;

    @LogMetier(champs = {"demande.formule", "demande.typeLogement", "resultat.tarifable"})
    public TarifHabitationResponse calculer(TarifHabitationRequest demande) {
        Instant maintenant = horloge.instant();
        LocalDate aujourdhui = LocalDate.ofInstant(maintenant, horloge.getZone());
        GrilleTarifaire grille = grilles.applicableAu(aujourdhui)
                .orElseThrow(() -> new GrilleIndisponibleException(aujourdhui));
        verifierFranchise(demande.franchise(), grille);
        verifierCodePostal(demande.codePostal(), grille);

        log.info("Grille tarifaire appliquée : version={}", grille.version());
        ProfilRisque profil = mapper.versProfil(demande);
        UUID idTarif = UUID.randomUUID();
        TarifHabitationResponse resultat = eligibilite.motifDeRefus(profil, grille.refus())
                .map(motif -> refus(idTarif, grille, motif))
                .orElseGet(() -> tarif(idTarif, demande, profil, grille, aujourdhui));

        trace.enregistrer(demande, resultat, maintenant);
        log.info("Calcul tarifaire terminé : idTarif={}, grille={}, tarifable={}, formuleConseillee={}",
                idTarif, grille.version(), resultat.tarifable(), resultat.formuleConseillee());
        return resultat;
    }

    @LogMetier
    public TarifHabitationResponse reprendre(UUID idTarif) {
        log.info("Reprise d'un tarif demandée : idTarif={}", idTarif);
        TarifHabitationResponse resultat = trace.relire(idTarif).orElseThrow(() -> new TarifInconnuException(idTarif));
        if (resultat.dateValidite() != null && resultat.dateValidite().isBefore(LocalDate.now(horloge))) {
            log.info("Reprise refusée, tarif expiré : idTarif={}", idTarif);
            throw new TarifExpireException(resultat.dateValidite());
        }
        log.info("Tarif repris à son prix d'origine : idTarif={}", idTarif);
        return resultat;
    }

    private TarifHabitationResponse refus(UUID idTarif, GrilleTarifaire grille, String motif) {
        log.info("Profil orienté vers un conseiller : idTarif={}", idTarif);
        return TarifHabitationResponse.builder()
                .idTarif(idTarif)
                .versionGrille(grille.version())
                .tarifable(false)
                .motif(motif)
                .orientation(Orientation.CONSEILLER)
                .build();
    }

    private TarifHabitationResponse tarif(
            UUID idTarif, TarifHabitationRequest demande, ProfilRisque profil, GrilleTarifaire grille, LocalDate aujourdhui) {
        TarifHabitationResponse.TarifHabitationResponseBuilder reponse = TarifHabitationResponse.builder()
                .idTarif(idTarif)
                .versionGrille(grille.version())
                .tarifable(true)
                .prix(calculateurPrix.prixDesFormules(profil, grille))
                .prixOptions(calculateurPrix.prixDesOptions(grille))
                .ajustementsFranchise(calculateurPrix.ajustementsDesFranchises(grille))
                .dateValidite(aujourdhui.plusDays(grille.dureeValiditeJours()));
        if (demande.formule() != null) {
            List<OptionTarifaire> options =
                    Optional.ofNullable(demande.options()).orElseGet(List::of);
            reponse.detail(calculateurPrix.detail(profil, demande.formule(), options, demande.franchise(), grille));
        }
        if (demande.besoins() != null) {
            Besoins besoins = mapper.versBesoins(demande.besoins());
            FormuleConseillee conseil = recommandation.formuleConseillee(besoins, grille.recommandation());
            reponse.formuleConseillee(conseil.formule())
                    .raisons(conseil.raisons())
                    .optionsConseillees(recommandation.optionsConseillees(besoins));
        }
        return reponse.build();
    }

    private void verifierCodePostal(String codePostal, GrilleTarifaire grille) {
        boolean couvert = grille.multiplicateurs().zones().stream()
                .flatMap(zone -> zone.departements().stream())
                .anyMatch(codePostal::startsWith);
        if (!couvert) {
            throw new DonneeInvalideException(CHAMP_CODE_POSTAL, CODE_POSTAL_NON_COUVERT);
        }
    }

    private void verifierFranchise(Integer franchise, GrilleTarifaire grille) {
        if (franchise != null && !grille.ajustementsFranchise().containsKey(franchise)) {
            String valeurs = grille.ajustementsFranchise().keySet().stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new DonneeInvalideException(CHAMP_FRANCHISE, "Franchise non proposée. Valeurs possibles : " + valeurs + ".");
        }
    }
}
