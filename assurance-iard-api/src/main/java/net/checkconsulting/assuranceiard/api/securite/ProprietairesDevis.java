package net.checkconsulting.assuranceiard.api.securite;

import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import org.springframework.stereotype.Component;

/** US-04 : un devis enregistré appartient au(x) compte(s) qui l'ont enregistré. */
@Component
public class ProprietairesDevis extends ProprietairesParReference {

    public ProprietairesDevis(DevisEnregistreRepository repository) {
        super(TypeRessource.DEVIS, repository::proprietaires);
    }
}
