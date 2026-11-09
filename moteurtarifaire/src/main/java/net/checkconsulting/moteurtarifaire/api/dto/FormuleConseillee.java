package net.checkconsulting.moteurtarifaire.api.dto;

import java.util.List;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;

public record FormuleConseillee(Formule formule, List<String> raisons) {
}
