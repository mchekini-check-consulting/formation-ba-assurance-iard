package net.checkconsulting.assuranceiard.api.securite;

import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import org.springframework.stereotype.Component;

/** US-04 : un contrat appartient au client de son dossier de souscription. */
@Component
public class ProprietairesContrat extends ProprietairesParReference {

    public ProprietairesContrat(ContratRepository repository) {
        super(TypeRessource.CONTRAT, repository::proprietaires);
    }
}
