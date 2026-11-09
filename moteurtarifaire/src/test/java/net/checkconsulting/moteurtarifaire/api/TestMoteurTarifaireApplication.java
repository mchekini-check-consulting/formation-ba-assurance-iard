package net.checkconsulting.moteurtarifaire.api;

import org.springframework.boot.SpringApplication;

public class TestMoteurTarifaireApplication {

	public static void main(String[] args) {
		SpringApplication.from(MoteurTarifaireApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
