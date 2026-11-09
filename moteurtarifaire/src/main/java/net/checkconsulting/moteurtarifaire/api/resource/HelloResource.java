package net.checkconsulting.moteurtarifaire.api.resource;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloResource {

    static final String MESSAGE = "Hello Moteur Tarifaire";

    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> direBonjour() {
        return ResponseEntity.ok(MESSAGE);
    }
}
