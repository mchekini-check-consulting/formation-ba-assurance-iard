package net.checkconsulting.assuranceiard.api.securite;

import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import org.springframework.stereotype.Component;

/** US-04 : un dossier de souscription appartient au compte qui l'a ouvert. */
@Component
public class ProprietairesSouscription extends ProprietairesParReference {

    public ProprietairesSouscription(DossierSouscriptionRepository repository) {
        super(TypeRessource.SOUSCRIPTION, repository::proprietaires);
    }
}
