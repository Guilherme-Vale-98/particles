package com.gui.particles;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class ParticlesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParticlesApplication.class, args);
	}

}
