package net.checkconsulting.assuranceiard.api.securite;

import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import org.springframework.stereotype.Component;

/** US-04 / US-18 : le contrat d'un devis appartient au client du contrat (contrat.compte_id). */
@Component
public class ProprietairesContrat extends ProprietairesParReference {

    public ProprietairesContrat(ContratRepository repository) {
        super(TypeRessource.CONTRAT, repository::proprietaires);
    }
}
