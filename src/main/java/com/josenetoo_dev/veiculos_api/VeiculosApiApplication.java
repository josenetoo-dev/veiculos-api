package com.josenetoo_dev.veiculos_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication(exclude = org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class)
public class VeiculosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(VeiculosApiApplication.class, args);
	}
}
