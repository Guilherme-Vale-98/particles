package com.gui.particles;

import org.springframework.boot.SpringApplication;

public class TestParticlesApplication {

	public static void main(String[] args) {
		SpringApplication.from(ParticlesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
