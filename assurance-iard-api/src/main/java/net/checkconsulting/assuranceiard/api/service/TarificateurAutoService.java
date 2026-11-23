package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient.DemandeMoteurAuto;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse.Detail;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse.PrixFormule;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse.PrixOption;
import net.checkconsulting.assuranceiard.api.exception.VehiculeIntrouvableException;
import net.checkconsulting.assuranceiard.api.model.RefPlaqueTest;
import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import net.checkconsulting.assuranceiard.api.repository.RefPlaqueTestRepository;
import net.checkconsulting.assuranceiard.api.repository.RefVehiculeRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TarificateurAutoService {

    private static final String COMPORTEMENT_TROUVE = "TROUVE";

    private final MoteurTarifaireClient client;
    private final RefVehiculeRepository vehicules;
    private final RefPlaqueTestRepository plaques;

    public TarifAutoResponse estimer(TarifAutoRequest demande) {
        RefVehicule vehicule = resoudreVehicule(demande);
        BigDecimal valeur = vehicule.getValeurNeuve();

        DemandeMoteurAuto requete = new DemandeMoteurAuto(
                demande.anneePermis(),
                demande.bonusMalus(),
                demande.kmAnnuels(),
                demande.usage(),
                demande.stationnement(),
                valeur,
                demande.formule(),
                demande.options());

        TarifAutoResponse moteurReponse = client.calculerAuto(requete);
        log.info("Tarif auto obtenu du moteur : idTarif={}, tarifable={}",
                moteurReponse.idTarif(), moteurReponse.tarifable());

        return enrichir(moteurReponse, vehicule, valeur);
    }

    private RefVehicule resoudreVehicule(TarifAutoRequest demande) {
        if (demande.refVehiculeId() != null) {
            return vehicules.findById(demande.refVehiculeId())
                    .orElseThrow(() -> new VehiculeIntrouvableException(
                            "Véhicule introuvable (id=" + demande.refVehiculeId() + ")."));
        }
        if (demande.immatriculation() != null) {
            RefPlaqueTest plaque = plaques.findByImmatriculation(demande.immatriculation())
                    .orElseThrow(() -> new VehiculeIntrouvableException(
                            "Véhicule introuvable pour la plaque " + demande.immatriculation() + "."));
            if (!COMPORTEMENT_TROUVE.equals(plaque.getComportement())) {
                throw new VehiculeIntrouvableException(
                        "Véhicule introuvable pour la plaque " + demande.immatriculation() + ".");
            }
            return vehicules.findById(plaque.getRefVehiculeId())
                    .orElseThrow(() -> new VehiculeIntrouvableException(
                            "Véhicule de référence introuvable pour la plaque " + demande.immatriculation() + "."));
        }
        throw new VehiculeIntrouvableException(
                "Identifiant du véhicule absent : fournir immatriculation ou refVehiculeId.");
    }

    private TarifAutoResponse enrichir(TarifAutoResponse moteur, RefVehicule vehicule, BigDecimal valeur) {
        return new TarifAutoResponse(
                moteur.idTarif(),
                moteur.versionGrille(),
                moteur.tarifable(),
                moteur.formuleConseillee(),
                moteur.prix() == null ? null : moteur.prix().stream()
                        .map(p -> new PrixFormule(p.formule(), p.mensuel(), p.annuel()))
                        .collect(Collectors.toList()),
                moteur.prixOptions() == null ? null : moteur.prixOptions().stream()
                        .map(o -> new PrixOption(o.option(), o.prixMensuel()))
                        .collect(Collectors.toList()),
                moteur.detail() == null ? null : new Detail(
                        moteur.detail().formule(),
                        moteur.detail().prixFormule(),
                        moteur.detail().options() == null ? null : moteur.detail().options().stream()
                                .map(o -> new PrixOption(o.option(), o.prixMensuel()))
                                .collect(Collectors.toList()),
                        moteur.detail().totalMensuel(),
                        moteur.detail().totalAnnuel()),
                moteur.dateValidite(),
                vehicule.getMarque(),
                vehicule.getModele(),
                valeur);
    }
}
