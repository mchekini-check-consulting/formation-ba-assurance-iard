package net.checkconsulting.assuranceiard.api.resource;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import net.checkconsulting.assuranceiard.api.securite.AccesPublic;

@AccesPublic
@RestController
public class HelloResource {

    static final String MESSAGE = "Hello Assurance IARD";

    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> direBonjour() {
        return ResponseEntity.ok(MESSAGE);
    }
}
