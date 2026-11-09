package net.checkconsulting.assuranceiard.api;

import org.springframework.boot.SpringApplication;

public class TestAssuranceIardApplication {

	public static void main(String[] args) {
		SpringApplication.from(AssuranceIardApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
